package br.com.archbase.security.config;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.security.adapter.AccessTokenPersistenceAdapter;
import br.com.archbase.security.auth.ArchbaseCurrentUser;
import br.com.archbase.security.auth.external.ExternalAuthenticationPolicy;
import br.com.archbase.security.auth.external.ExternalAuthenticationRejectedException;
import br.com.archbase.security.auth.external.ExternalBearerAuthenticator;
import br.com.archbase.security.auth.external.ExternalTokenCheck;
import br.com.archbase.security.domain.entity.ApiToken;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.AccessTokenEntity;
import br.com.archbase.security.repository.AccessTokenJpaRepository;
import br.com.archbase.security.service.ApiTokenService;
import br.com.archbase.security.service.ArchbaseJwtService;
import br.com.archbase.security.spi.ArchbaseCompanyAccessResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Enumeration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArchbaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final ArchbaseJwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AccessTokenJpaRepository tokenRepository;
    private final ApiTokenService apiTokenService;
    private final AccessTokenPersistenceAdapter accessTokenPersistenceAdapter;

    /**
     * Mesma chave que o {@code ArchbaseTenantRequestInterceptor} usa. Ler o tenant da query
     * string deixa o valor em log de acesso, histórico de proxy e Referer; desligar a chave
     * precisa fechar os dois pontos, não só o interceptor.
     */
    @Value("${archbase.app.tenant.accept-query-param:true}")
    private boolean acceptTenantQueryParam;

    @Value("${archbase.app.tenant.default.id:}")
    private String defaultTenantId;

    /**
     * Aceita a credencial em {@code ?token=} da URL.
     *
     * <p>Continua ligado por compatibilidade (downloads e SSE costumam depender disso, porque não
     * conseguem mandar header), mas token em query string acaba gravado no access log do servidor,
     * no histórico do navegador e no {@code Referer} enviado a terceiros. Prefira header
     * {@code Authorization} e desligue com
     * {@code archbase.security.jwt.accept-token-query-param=false}.
     */
    @Value("${archbase.security.jwt.accept-token-query-param:true}")
    private boolean acceptTokenQueryParam;

    /**
     * Autenticadores de bearer token externos (ex.: {@code archbase-controlplane}). Injetados por setter, e não pelo
     * construtor, para não mudar a assinatura de quem instancia o filtro. Vazio = comportamento de sempre.
     */
    private List<ExternalBearerAuthenticator> externalAuthenticators = List.of();

    /**
     * Conferência do {@code X-COMPANY-ID} contra o usuário autenticado: {@code off} (padrão, comportamento de
     * sempre), {@code warn} (só registra) ou {@code enforce} (recusa com {@code 403 COMPANY_NOT_ALLOWED}).
     *
     * @since 3.5.0
     */
    @Value("${archbase.security.company.validation:off}")
    private String companyValidation = COMPANY_VALIDATION_OFF;

    static final String COMPANY_VALIDATION_OFF = "off";
    static final String COMPANY_VALIDATION_WARN = "warn";
    static final String COMPANY_VALIDATION_ENFORCE = "enforce";
    public static final String COMPANY_NOT_ALLOWED = "COMPANY_NOT_ALLOWED";

    private List<ArchbaseCompanyAccessResolver> companyAccessResolvers = List.of();

    /** @since 3.5.0 */
    @Autowired(required = false)
    public void setCompanyAccessResolvers(List<ArchbaseCompanyAccessResolver> resolvers) {
        this.companyAccessResolvers = resolvers == null ? List.of() : List.copyOf(resolvers);
    }

    /** Para testes e para quem instancia o filtro à mão. */
    public void setCompanyValidation(String mode) {
        this.companyValidation = mode == null ? COMPANY_VALIDATION_OFF : mode;
    }

    /** Política efetiva dos provedores externos (já combinada); {@code null} = comportamento de sempre. */
    private ExternalAuthenticationPolicy externalPolicy;

    @Autowired(required = false)
    public void setExternalAuthenticators(List<ExternalBearerAuthenticator> externalAuthenticators) {
        this.externalAuthenticators = externalAuthenticators == null ? List.of() : List.copyOf(externalAuthenticators);
    }

    /**
     * Todas as políticas registradas, combinadas por {@link #combine}. Era um único bean injetado: com dois provedores
     * externos (ex.: Control Plane e IdP OIDC) a injeção era ambígua e o contexto não subia. Com zero ou um bean o
     * resultado é idêntico ao de antes.
     *
     * @since 3.5.0
     */
    @Autowired(required = false)
    public void setExternalPolicies(List<ExternalAuthenticationPolicy> policies) {
        this.externalPolicy = combine(policies);
    }

    /** Define uma única política. Mantido por compatibilidade; o contexto injeta por {@link #setExternalPolicies}. */
    public void setExternalPolicy(ExternalAuthenticationPolicy externalPolicy) {
        this.externalPolicy = externalPolicy;
    }

    /**
     * Combina políticas de forma conservadora: cada permissão só vale se <b>todas</b> concederem. Um provedor que
     * recusa o JWT legado ou o tenant padrão não pode ser desfeito por outro que aceite.
     *
     * @return {@code null} sem políticas (comportamento de sempre); a própria política quando só há uma
     */
    static ExternalAuthenticationPolicy combine(List<ExternalAuthenticationPolicy> policies) {
        if (policies == null || policies.isEmpty()) {
            return null;
        }
        if (policies.size() == 1) {
            return policies.get(0);
        }
        List<ExternalAuthenticationPolicy> all = List.copyOf(policies);
        return new ExternalAuthenticationPolicy() {
            @Override
            public boolean acceptLegacyJwt() {
                return all.stream().allMatch(ExternalAuthenticationPolicy::acceptLegacyJwt);
            }

            @Override
            public boolean allowDefaultTenant() {
                return all.stream().allMatch(ExternalAuthenticationPolicy::allowDefaultTenant);
            }
        };
    }

    public static final String X_TENANT_ID = "X-TENANT-ID";
    public static final String X_COMPANY_ID = "X-COMPANY-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String tokenParam = acceptTokenQueryParam ? request.getParameter("token") : null;

        try {
            log.debug("Requisição recebida: {} {}", request.getMethod(), request.getRequestURI());
            logAllHeaders(request);

            // Token emitido fora do Archbase: o autenticador externo assume a requisição inteira e o
            // fluxo abaixo (tenant padrão, JWT HS256, token de API) não roda. Sem autenticador
            // registrado, findExternal devolve null e nada aqui muda.
            String bearer = bearerJwt(authHeader, tokenParam);
            ExternalBearerAuthenticator external = findExternal(bearer);
            if (external != null) {
                if (authenticateExternally(external, bearer, request, response)
                        && companyAllowed(request, response)) {
                    filterChain.doFilter(request, response);
                }
                return;
            }

            // O fallback para a query string obedece archbase.app.tenant.accept-query-param.
            // Antes ele era incondicional aqui, e como este filtro roda ANTES do
            // ArchbaseTenantRequestInterceptor — o único que consultava a flag — desligá-la não
            // tinha efeito nenhum: ?X-TENANT-ID=outro seguia definindo o tenant de qualquer
            // requisição. O passo de endurecimento documentado dava por fechado um buraco aberto.
            String tenantId = request.getHeader(X_TENANT_ID);
            if ((tenantId == null || tenantId.isEmpty()) && acceptTenantQueryParam) {
                tenantId = request.getParameter(X_TENANT_ID);
            }
            log.debug("TenantID recebido: {}", tenantId);

            String companyId = request.getHeader(X_COMPANY_ID);
            if ((companyId == null || companyId.isEmpty()) && acceptTenantQueryParam) {
                companyId = request.getParameter(X_COMPANY_ID);
            }
            log.debug("CompanyID recebido: {}", companyId);

            if (tenantId != null && !tenantId.isEmpty()) {
                ArchbaseTenantContext.setTenantId(tenantId);
                log.debug("TenantID definido no contexto: {}", tenantId);
            } else if (externalPolicy != null && !externalPolicy.allowDefaultTenant()) {
                // O provedor externo proíbe o tenant padrão. Para o JWT próprio do Archbase, o tenant
                // vem do claim assinado do token — o mesmo que a reconciliação abaixo já trata como
                // fonte de verdade. Sem isso, cliente legado que não manda X-TENANT-ID perderia o
                // acesso ao ligar o provedor externo, porque o usuário seria buscado sem tenant.
                String doToken = tenantFromLegacyJwt(authHeader, tokenParam);
                if (doToken != null && !doToken.isEmpty()) {
                    ArchbaseTenantContext.setTenantId(doToken);
                    log.debug("TenantID tirado do claim do JWT (tenant padrão desligado): {}", doToken);
                }
            } else if (defaultTenantId != null && !defaultTenantId.isEmpty()) {
                ArchbaseTenantContext.setTenantId(defaultTenantId);
                log.debug("TenantID não fornecido na requisição, utilizando tenant padrão: {}", defaultTenantId);
            } else {
                log.debug("TenantID não fornecido na requisição e nenhum tenant padrão configurado (archbase.app.tenant.default.id)");
            }

            if (companyId != null && !companyId.isEmpty()) {
                ArchbaseTenantContext.setCompanyId(companyId);
                log.debug("CompanyID definido no contexto: {}", companyId);
            }

            log.debug("Authorization header presente: {}", authHeader != null);
            log.debug("Token parameter presente: {}", tokenParam != null);

            // Tenant do token de API, quando a autenticação vier por essa via. Guardado aqui porque
            // a reconciliação de tenant, mais abaixo, precisa dele.
            String apiTokenTenantId = null;

            // Processar header de autorização
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (isValidUUID(token)) {
                        // Token de API apresentado como Bearer — a forma padrão de mandar um token
                        // opaco (RFC 6750), e o que todo cliente HTTP genérico faz. Antes só era
                        // reconhecido cru no header: quem seguia o padrão levava 401 sem
                        // explicação, porque o valor seguia para o parser de JWT e falhava lá.
                        log.debug("Processando API token via Bearer: {}", maskUUID(token));
                        apiTokenTenantId = processApiToken(token, request);
                    } else {
                        // Token JWT (usuário/senha)
                        log.debug("Processando Bearer token: {}", maskToken(token));
                        processJwtToken(token, request);
                    }
                } else if (isValidUUID(authHeader)) {
                    // Token API (UUID direto)
                    log.debug("Processando API token (UUID): {}", maskUUID(authHeader));
                    apiTokenTenantId = processApiToken(authHeader, request);
                } else {
                    log.warn("Formato de autorização não reconhecido: {}", maskAuthHeader(authHeader));
                }
            }
            // Se não tem header, tenta pegar token JWT da URL
            else if (tokenParam != null) {
                if (isValidUUID(tokenParam)) {
                    log.debug("Processando API token da URL (UUID): {}", maskUUID(tokenParam));
                    apiTokenTenantId = processApiToken(tokenParam, request);
                } else {
                    log.debug("Processando JWT token da URL: {}", maskToken(tokenParam));
                    processJwtToken(tokenParam, request);
                }
            } else {
                log.debug("Nenhum token de autenticação encontrado na requisição");
            }

            // Log de diagnóstico - autenticação
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("Usuário autenticado: {}, Autoridades: {}",
                        SecurityContextHolder.getContext().getAuthentication().getName(),
                        SecurityContextHolder.getContext().getAuthentication().getAuthorities());
            } else {
                log.debug("Nenhuma autenticação definida após processamento de token");
            }

            // Isolamento tenant↔token: o tenant do token é a fonte de verdade.
            // Se o X-TENANT-ID do header divergir, rejeita com 403 (impede acesso cross-tenant).
            // Tokens legados (sem o claim) mantêm o comportamento anterior (fallback pelo header).
            //
            // Vale para JWT (claim assinado) e para token de API (tenant da própria linha). O token
            // de API ficava de fora: resolveJwtForTenant devolve null para UUID, então o bloco
            // inteiro era pulado e o contexto seguia com o tenant que o cliente mandou no header.
            String tokenTenant = apiTokenTenantId != null
                    ? apiTokenTenantId
                    : resolveTenantFromJwt(authHeader, tokenParam);

            if (tokenTenant != null && !tokenTenant.isEmpty()
                    && SecurityContextHolder.getContext().getAuthentication() != null) {
                if (tenantId != null && !tenantId.isEmpty() && !tenantId.equals(tokenTenant)) {
                    log.warn("Acesso cross-tenant NEGADO: usuario={}, tenantDoToken={}, X-TENANT-ID={}, {} {}",
                            SecurityContextHolder.getContext().getAuthentication().getName(),
                            tokenTenant, tenantId, request.getMethod(), request.getRequestURI());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "X-TENANT-ID não corresponde ao tenant do token");
                    return;
                }
                // Fonte de verdade: alinha o contexto ao tenant do token.
                ArchbaseTenantContext.setTenantId(tokenTenant);
            }

            if (!companyAllowed(request, response)) {
                return;
            }

            filterChain.doFilter(request, response);
        } catch (ServletException | IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Log detalhado, mas sem interromper o fluxo de filtros
            log.error("Erro ao processar autenticação: {}", e.getMessage(), e);
        } finally {
            ArchbaseTenantContext.clear();
        }
    }

    /**
     * Confere o {@code X-COMPANY-ID} que já está no contexto contra o usuário autenticado, conforme
     * {@code archbase.security.company.validation}. Roda depois da autenticação e do alinhamento do tenant, para os três
     * caminhos (JWT próprio, token de API e token externo).
     * <ul>
     * <li>sem empresa no contexto, ou modo {@code off}: nada muda;</li>
     * <li>sem usuário autenticado (rota pública): com {@code enforce} a empresa é descartada do contexto — ninguém
     * para conferir, e o cabeçalho de um anônimo não pode escolher a empresa dos dados; com {@code warn}, só log;</li>
     * <li>autenticado: o {@link ArchbaseCompanyAccessResolver} decide. Principal que não é {@link UserEntity}, falta de
     * resolver ou exceção contam como recusa.</li>
     * </ul>
     *
     * @return {@code true} se a requisição pode seguir; {@code false} se o 403 já foi escrito
     */
    boolean companyAllowed(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String mode = companyValidation == null ? COMPANY_VALIDATION_OFF : companyValidation.trim().toLowerCase();
        if (COMPANY_VALIDATION_OFF.equals(mode)) {
            return true;
        }
        boolean enforce = COMPANY_VALIDATION_ENFORCE.equals(mode);
        String companyId = ArchbaseTenantContext.getCompanyId();
        if (companyId == null || companyId.isEmpty()) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            if (enforce) {
                ArchbaseTenantContext.setCompanyId(null);
                request.setAttribute(ArchbaseTenantContext.COMPANY_DISCARDED_ATTRIBUTE, Boolean.TRUE);
                log.debug("X-COMPANY-ID descartado: requisição sem usuário autenticado ({} {})",
                        request.getMethod(), request.getRequestURI());
            } else {
                log.warn("[company.validation=warn] X-COMPANY-ID={} em requisição sem usuário autenticado; com "
                        + "enforce ele seria descartado. {} {}", companyId, request.getMethod(), request.getRequestURI());
            }
            return true;
        }

        Optional<UserEntity> user = ArchbaseCurrentUser.of(authentication);
        String motivo = null;
        if (user.isEmpty()) {
            motivo = "principal não é um usuário local";
        } else if (companyAccessResolvers.isEmpty()) {
            motivo = "nenhum ArchbaseCompanyAccessResolver registrado";
        } else {
            try {
                boolean permitido = false;
                for (ArchbaseCompanyAccessResolver resolver : companyAccessResolvers) {
                    if (resolver.canAccess(user.get(), companyId)) {
                        permitido = true;
                        break;
                    }
                }
                if (!permitido) {
                    motivo = "usuário sem acesso à empresa";
                }
            } catch (RuntimeException e) {
                log.error("ArchbaseCompanyAccessResolver falhou: {}", e.getMessage(), e);
                motivo = "falha ao conferir o acesso à empresa";
            }
        }
        if (motivo == null) {
            return true;
        }
        if (!enforce) {
            log.warn("[company.validation=warn] X-COMPANY-ID={} seria recusado ({}): usuario={}, {} {}",
                    companyId, motivo, authentication.getName(), request.getMethod(), request.getRequestURI());
            return true;
        }
        log.warn("Acesso à empresa NEGADO ({}): usuario={}, X-COMPANY-ID={}, {} {}", motivo,
                authentication.getName(), companyId, request.getMethod(), request.getRequestURI());
        writeExternalError(response, HttpServletResponse.SC_FORBIDDEN, COMPANY_NOT_ALLOWED,
                "X-COMPANY-ID não é uma empresa acessível a este usuário");
        return false;
    }

    /** O JWT apresentado (Bearer ou {@code ?token=}), ou {@code null} se não houver ou se for token de API. */
    private String bearerJwt(String authHeader, String tokenParam) {
        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return isValidUUID(token) ? null : token;
            }
            return null;
        }
        return tokenParam != null && !isValidUUID(tokenParam) ? tokenParam : null;
    }

    private ExternalBearerAuthenticator findExternal(String token) {
        if (token == null || externalAuthenticators.isEmpty()) {
            return null;
        }
        for (ExternalBearerAuthenticator candidate : externalAuthenticators) {
            try {
                if (candidate.supports(token)) {
                    return candidate;
                }
            } catch (RuntimeException e) {
                log.warn("Autenticador externo {} falhou ao inspecionar o token: {}",
                        candidate.getClass().getName(), e.getMessage());
            }
        }
        return null;
    }

    /**
     * Autentica pelo provedor externo, com a regra de tenant aplicada aqui: o tenant do token é a fonte de verdade,
     * um {@code X-TENANT-ID} divergente é recusado, e o tenant padrão nunca é usado.
     *
     * @return {@code true} se a requisição pode seguir; {@code false} se a resposta de erro já foi escrita
     */
    private boolean authenticateExternally(ExternalBearerAuthenticator external, String token,
                                           HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ExternalTokenCheck check;
        try {
            check = external.check(token, request);
        } catch (RuntimeException e) {
            log.error("Autenticador externo {} falhou ao validar o token: {}",
                    external.getClass().getName(), e.getMessage(), e);
            check = ExternalTokenCheck.rejected(HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN",
                    "Token inválido");
        }
        if (check == null || !check.isAccepted()) {
            ExternalTokenCheck rejected = check != null ? check : ExternalTokenCheck.rejected(
                    HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Token inválido");
            rejectExternally(external, request, response, rejected.status(), rejected.code(), rejected.message());
            return false;
        }

        String headerTenant = request.getHeader(X_TENANT_ID);
        if ((headerTenant == null || headerTenant.isEmpty()) && acceptTenantQueryParam) {
            headerTenant = request.getParameter(X_TENANT_ID);
        }
        String tenant = check.tenantId();
        if (tenant == null || tenant.isBlank()) {
            if (headerTenant == null || headerTenant.isBlank()) {
                rejectExternally(external, request, response, HttpServletResponse.SC_FORBIDDEN, "TENANT_REQUIRED",
                        "A requisição não identifica o tenant");
                return false;
            }
            tenant = headerTenant;
        } else if (headerTenant != null && !headerTenant.isEmpty() && !headerTenant.equals(tenant)) {
            log.warn("Acesso cross-tenant NEGADO (token externo): tenantDoToken={}, X-TENANT-ID={}, {} {}",
                    tenant, headerTenant, request.getMethod(), request.getRequestURI());
            rejectExternally(external, request, response, HttpServletResponse.SC_FORBIDDEN, "TENANT_MISMATCH",
                    "X-TENANT-ID não corresponde ao tenant do token");
            return false;
        }
        ArchbaseTenantContext.setTenantId(tenant);

        String companyId = request.getHeader(X_COMPANY_ID);
        if ((companyId == null || companyId.isEmpty()) && acceptTenantQueryParam) {
            companyId = request.getParameter(X_COMPANY_ID);
        }
        if (companyId != null && !companyId.isEmpty()) {
            ArchbaseTenantContext.setCompanyId(companyId);
        }

        Authentication authentication;
        try {
            authentication = external.authenticate(check, request);
        } catch (ExternalAuthenticationRejectedException e) {
            rejectExternally(external, request, response, e.getStatus(), e.getCode(), e.getMessage());
            return false;
        } catch (RuntimeException e) {
            log.error("Autenticador externo {} falhou ao montar a autenticação: {}",
                    external.getClass().getName(), e.getMessage(), e);
            authentication = null;
        }
        if (authentication == null) {
            rejectExternally(external, request, response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Token inválido");
            return false;
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return true;
    }

    private static void rejectExternally(ExternalBearerAuthenticator external, HttpServletRequest request,
                                         HttpServletResponse response, int status, String code, String message)
            throws IOException {
        try {
            external.onRejected(code, request);
        } catch (RuntimeException e) {
            log.debug("onRejected de {} falhou: {}", external.getClass().getName(), e.getMessage());
        }
        writeExternalError(response, status, code, message);
    }

    private static void writeExternalError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status
                + ",\"error\":\"" + jsonEscape(code) + "\""
                + ",\"code\":\"" + jsonEscape(code) + "\""
                + ",\"message\":\"" + jsonEscape(message) + "\"}");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /** {@link #resolveTenantFromJwt}, sem deixar falha de leitura do token interromper a requisição. */
    private String tenantFromLegacyJwt(String authHeader, String tokenParam) {
        try {
            return resolveTenantFromJwt(authHeader, tokenParam);
        } catch (RuntimeException e) {
            log.debug("Não foi possível ler o tenant do JWT: {}", e.getMessage());
            return null;
        }
    }

    /** Tenant declarado no claim do JWT (Bearer ou via URL); {@code null} para API token (UUID). */
    private String resolveTenantFromJwt(String authHeader, String tokenParam) {
        if (authHeader != null && authHeader.startsWith("Bearer ") && !isValidUUID(authHeader.substring(7))) {
            return jwtService.extractTenantId(authHeader.substring(7));
        }
        if (tokenParam != null && !isValidUUID(tokenParam)) {
            return jwtService.extractTenantId(tokenParam);
        }
        return null;
    }

    private void processJwtToken(String token, HttpServletRequest request) {
        if (externalPolicy != null && !externalPolicy.acceptLegacyJwt()) {
            log.warn("JWT próprio do Archbase recusado: o provedor externo exige o token novo "
                    + "(archbase.controlplane.legacy-jwt=reject)");
            return;
        }
        try {
            // Refresh token e desafio de MFA são JWTs assinados com o mesmo subject do access
            // token: sem esta checagem, ambos passariam por credencial. O refresh agora é
            // persistido na mesma tabela consultada abaixo, então a busca em banco deixou de ser,
            // sozinha, o que os separava.
            if (!jwtService.isAccessToken(token)) {
                log.warn("Token apresentado não é um access token — autenticação recusada");
                return;
            }

            // Extrai o email do usuário
            String userEmail = jwtService.extractUsername(token);
            log.debug("Email extraído do JWT: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Busca detalhes do usuário
                log.debug("Buscando detalhes do usuário: {}", userEmail);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.debug("Usuário encontrado: {}, Autoridades: {}", userDetails.getUsername(), userDetails.getAuthorities());

                // Verifica se o token existe no banco e é válido
                AccessTokenEntity tokenEntity = accessTokenPersistenceAdapter.findTokenByValue(token);
                log.debug("Token encontrado no banco: {}", tokenEntity != null);

                boolean isTokenValid = false;

                if (tokenEntity != null) {
                    isTokenValid = !tokenEntity.isExpired() && !tokenEntity.isRevoked();
                    log.debug("Token válido: {}, Expirado: {}, Revogado: {}",
                            isTokenValid, tokenEntity.isExpired(), tokenEntity.isRevoked());
                }

                // Valida o token JWT
                boolean isJwtValid = jwtService.isTokenValid(token, userDetails);
                log.debug("JWT válido: {}", isJwtValid);

                if (isJwtValid && isTokenValid) {
                    setAuthentication(userDetails, request);
                    log.debug("Autenticação JWT bem-sucedida para usuário: {}", userEmail);
                } else {
                    log.warn("Token JWT inválido para usuário: {}, JWT válido: {}, Token válido: {}",
                            userEmail, isJwtValid, isTokenValid);
                    // Assinatura boa, mas a sessão foi revogada no banco: logout, ou outro login do
                    // mesmo usuário de sessão única. Sem marcar o motivo, o 401 sai igual a uma
                    // credencial errada e o cliente mostra "usuário/senha não encontrados".
                    // O findTokenByValue só devolve token vigente; o revogado é buscado à parte.
                    if (isJwtValid && tokenEntity == null && tokenRepository.findByToken(token)
                            .map(AccessTokenEntity::isRevoked).orElse(false)) {
                        request.setAttribute(CustomAuthenticationEntryPoint.ATRIBUTO_MOTIVO,
                                CustomAuthenticationEntryPoint.SESSAO_ENCERRADA);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar token JWT: {}", e.getMessage(), e);
        }
    }

    /**
     * Autentica por token de API.
     *
     * @return o tenant ao qual o token pertence, para a reconciliação de tenant do
     *         {@link #doFilterInternal}; {@code null} se não autenticou ou se o token não tem tenant.
     */
    private String processApiToken(String token, HttpServletRequest request) {
        try {
            boolean isValid = apiTokenService.validateToken(token);
            log.debug("API Token válido: {}", isValid);

            if (isValid) {
                Optional<ApiToken> apiToken = apiTokenService.getApiToken(token);
                log.debug("API Token encontrado: {}", apiToken.isPresent());

                if (apiToken.isPresent()) {
                    boolean isActivated = apiToken.get().isActivated();
                    log.debug("API Token ativo: {}", isActivated);

                    if (isActivated) {
                        String userEmail = apiToken.get().getUser().getEmail();
                        log.debug("Email do usuário do API Token: {}", userEmail);

                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                        log.debug("Usuário encontrado: {}, Autoridades: {}",
                                userDetails.getUsername(), userDetails.getAuthorities());

                        setAuthentication(userDetails, request);
                        log.debug("Autenticação API Token bem-sucedida para usuário: {}", userEmail);
                        return apiToken.get().getTenantId();
                    } else {
                        log.warn("API Token está inativo: {}", maskUUID(token));
                    }
                } else {
                    log.warn("API Token não encontrado: {}", maskUUID(token));
                }
            } else {
                log.warn("API Token inválido: {}", maskUUID(token));
            }
        } catch (Exception e) {
            log.error("Erro ao processar API token: {}", e.getMessage(), e);
        }
        return null;
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug("Autenticação definida no SecurityContext: {}", userDetails.getUsername());
    }

    private boolean isValidUUID(String token) {
        try {
            UUID.fromString(token);
            return true;
        } catch (IllegalArgumentException e) {
            // Sem o valor: este método recebe a credencial crua, e TRACE acaba ligado em produção
            // com mais frequência do que se imagina.
            log.trace("Token apresentado não é um UUID");
            return false;
        }
    }

    /**
     * Registra todos os headers da requisição para diagnóstico
     */
    private void logAllHeaders(HttpServletRequest request) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("--- Headers da requisição ---");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);

            // Oculta informações sensíveis
            if (headerName.equalsIgnoreCase("Authorization")) {
                headerValue = maskAuthHeader(headerValue);
            } else if (isSensitiveHeader(headerName)) {
                // Só Authorization era mascarado. Cookie de sessão, chave de API e token de proxy
                // saíam inteiros no log de DEBUG — credenciais completas em texto puro.
                headerValue = "***";
            }

            log.debug("Header: {} = {}", headerName, headerValue);
        }
        log.debug("---------------------------");
    }

    /** Headers que carregam credencial e nunca devem aparecer inteiros no log. */
    private boolean isSensitiveHeader(String headerName) {
        String lower = headerName.toLowerCase();
        return lower.equals("cookie")
                || lower.equals("set-cookie")
                || lower.equals("proxy-authorization")
                || lower.contains("api-key")
                || lower.contains("apikey")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("password");
    }

    /**
     * Mascara tokens para exibição segura nos logs
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***token muito curto***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }

    /**
     * Mascara UUIDs para exibição segura nos logs
     */
    private String maskUUID(String uuid) {
        if (uuid == null || uuid.length() < 10) {
            return "***uuid muito curto***";
        }
        return uuid.substring(0, 5) + "..." + uuid.substring(uuid.length() - 5);
    }

    /**
     * Mascara o header de Authorization para exibição segura nos logs
     */
    private String maskAuthHeader(String authHeader) {
        if (authHeader == null) {
            return null;
        }

        if (authHeader.startsWith("Bearer ")) {
            return "Bearer " + maskToken(authHeader.substring(7));
        } else if (isValidUUID(authHeader)) {
            return maskUUID(authHeader);
        }

        return "***authorization mascarada***";
    }
}

package br.com.archbase.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class ArchbaseJwtService {

    /**
     * Claim que carrega o tenant do usuário no token (isolamento tenant↔token). O token é assinado,
     * então este claim é a fonte de verdade do tenant — não pode ser forjado pelo cliente, e não
     * depende de re-consultar o banco (que já estaria filtrado pelo tenant do header).
     */
    public static final String TENANT_CLAIM = "tenantId";

    /**
     * Claim que declara para que serve o token. Sem ele, todo token emitido é apenas "um JWT
     * assinado com o subject do usuário" — e qualquer endpoint que só confira assinatura + subject
     * aceita qualquer um deles. Era assim que o desafio de MFA (senha certa, segundo fator ainda
     * não apresentado) passava no {@code /auth/refresh-token} e voltava com os tokens reais.
     *
     * <p>Nome {@code token_use} em vez de {@code typ} de propósito: {@code typ} é um parâmetro
     * registrado do <i>header</i> JOSE, e reaproveitá-lo como claim gera confusão na leitura.
     */
    public static final String TOKEN_USE_CLAIM = "token_use";
    public static final String TOKEN_USE_ACCESS = "access";
    public static final String TOKEN_USE_REFRESH = "refresh";
    public static final String TOKEN_USE_MFA_CHALLENGE = "mfa_challenge";

    @Value("${archbase.security.jwt.secret-key}")
    private String secretKey;

    @Value("${archbase.security.jwt.token-expiration}")
    private long jwtExpiration;

    @Value("${archbase.security.jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Quando {@code true}, um token sem o claim {@value #TOKEN_USE_CLAIM} deixa de ser aceito.
     *
     * <p>Fica desligado por padrão porque os tokens emitidos antes desta versão não têm o claim:
     * ligá-lo num deploy derrubaria toda sessão em curso. Ligue depois que o maior
     * {@code refresh-expiration} configurado tiver passado desde a atualização — daí em diante
     * nenhum token legado sobrevive e a checagem pode ser estrita.
     */
    @Value("${archbase.security.jwt.strict-token-use:false}")
    private boolean strictTokenUse;

    @PostConstruct
    public void initialize() {
        if (StringUtils.isEmpty(secretKey)) {
            log.warn("Define uma chave secreta para a autenticação. Defina a propriedade 'archbase.security.jwt.secret-key'.");
            return;
        }
        if (jwtExpiration==0) {
            log.warn("Defina o tempo de expiração dos tokens de acesso. Defina a propriedade 'archbase.security.jwt.token-expiration'.");
            return;
        }
        if (refreshExpiration==0) {
            log.warn("Defina o tempo de expiração para refresh dos tokens de acesso. Defina a propriedade 'archbase.security.jwt.refresh-expiration'.");
            return;
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrai o tenant do claim {@value #TENANT_CLAIM} do token. Retorna {@code null} se o token
     * não tiver o claim (token legado, emitido antes desta mudança) ou não for um JWT válido.
     */
    public String extractTenantId(String token) {
        try {
            Object value = extractClaim(token, claims -> claims.get(TENANT_CLAIM));
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Obtém o tenantId do {@link UserDetails} via {@code getTenantId()} (reflection), se houver. */
    private String resolveTenantId(UserDetails userDetails) {
        try {
            Method method = userDetails.getClass().getMethod("getTenantId");
            Object value = method.invoke(userDetails);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public TokenResult generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public TokenResult generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(TOKEN_USE_CLAIM, TOKEN_USE_ACCESS);
        return buildToken(claims, userDetails, jwtExpiration);
    }

    public TokenResult generateRefreshToken(
            UserDetails userDetails
    ) {
        return buildToken(Map.of(TOKEN_USE_CLAIM, TOKEN_USE_REFRESH), userDetails, refreshExpiration);
    }

    /** Claim que marca um token de desafio de MFA (não serve como access token). */
    public static final String MFA_PURPOSE_CLAIM = "mfa";
    public static final String MFA_CHALLENGE_VALUE = "challenge";
    /** Validade do token de desafio de MFA (5 minutos). */
    private static final long MFA_CHALLENGE_EXPIRATION = 5 * 60 * 1000L;

    /**
     * Token curto de desafio emitido quando a senha confere mas o usuário tem MFA: carrega
     * {@code mfa=challenge} e nunca é persistido em access_token, logo o filtro JWT não o
     * aceita como credencial (findTokenByValue = null). Só o {@code /auth/mfa/verify} o consome.
     */
    public TokenResult generateMfaChallengeToken(UserDetails userDetails) {
        return buildToken(
                Map.of(MFA_PURPOSE_CLAIM, MFA_CHALLENGE_VALUE, TOKEN_USE_CLAIM, TOKEN_USE_MFA_CHALLENGE),
                userDetails,
                MFA_CHALLENGE_EXPIRATION);
    }

    /** Verdadeiro se o token é um desafio de MFA válido (claim correto + não expirado). */
    public boolean isMfaChallengeToken(String token) {
        try {
            Object purpose = extractClaim(token, claims -> claims.get(MFA_PURPOSE_CLAIM));
            return MFA_CHALLENGE_VALUE.equals(purpose) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lê o claim {@value #TOKEN_USE_CLAIM}. {@code null} para token legado (emitido antes do claim
     * existir) ou para entrada que não seja um JWT válido.
     */
    public String extractTokenUse(String token) {
        try {
            Object value = extractClaim(token, claims -> claims.get(TOKEN_USE_CLAIM));
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verdadeiro se o token pode ser apresentado como <b>credencial de acesso</b>.
     *
     * <p>Rejeita refresh token e desafio de MFA. Token legado (sem o claim) é aceito enquanto
     * {@code archbase.security.jwt.strict-token-use} estiver desligado.
     */
    public boolean isAccessToken(String token) {
        return isTokenUse(token, TOKEN_USE_ACCESS);
    }

    /**
     * Verdadeiro se o token pode ser apresentado em {@code /auth/refresh-token}.
     *
     * <p>É esta checagem que fecha o bypass de MFA: o desafio carrega
     * {@code token_use=mfa_challenge} e, mesmo se for um token legado sem o claim, ainda carrega
     * {@code mfa=challenge} — os dois caminhos caem fora daqui.
     */
    public boolean isRefreshToken(String token) {
        if (isMfaChallengeClaimPresent(token)) {
            return false;
        }
        return isTokenUse(token, TOKEN_USE_REFRESH);
    }

    private boolean isTokenUse(String token, String expected) {
        Claims claims;
        try {
            claims = extractAllClaims(token);
        } catch (Exception e) {
            // Não é um JWT válido para esta chave. A tolerância a token legado abaixo vale para
            // token nosso e antigo — não para entrada arbitrária, que precisa cair aqui.
            return false;
        }

        Object use = claims.get(TOKEN_USE_CLAIM);
        if (use == null) {
            // Token legado: sem o claim não há como distinguir o uso. Aceita para não invalidar
            // sessões em curso na atualização; strict-token-use=true remove esta tolerância.
            return !strictTokenUse;
        }
        return expected.equals(use.toString());
    }

    /** Presença do claim {@code mfa}, independente de expiração — usado para recusar o desafio. */
    private boolean isMfaChallengeClaimPresent(String token) {
        try {
            return MFA_CHALLENGE_VALUE.equals(extractClaim(token, claims -> claims.get(MFA_PURPOSE_CLAIM)));
        } catch (Exception e) {
            return false;
        }
    }

    private TokenResult buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        // Embute o tenant do usuário como claim (fonte de verdade do tenant), salvo se já informado.
        Map<String, Object> claims = new HashMap<>(extraClaims);
        String tenantId = resolveTenantId(userDetails);
        if (tenantId != null && !tenantId.isEmpty()) {
            claims.putIfAbsent(TENANT_CLAIM, tenantId);
        }

        // Usando UTC explicitamente
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusMillis(expiration));

        String token = Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .id(UUID.randomUUID().toString()) // jti: garante unicidade mesmo se gerado no mesmo segundo
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();

        // Retorna o timestamp UTC em milissegundos
        return new TokenResult(token, expiresAt.toInstant().toEpochMilli());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.toInstant().isBefore(Instant.now());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public record TokenResult(String token, Long expiresIn) {
    }
}



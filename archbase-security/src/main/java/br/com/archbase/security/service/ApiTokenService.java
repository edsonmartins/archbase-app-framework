    package br.com.archbase.security.service;

    import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
    import br.com.archbase.security.adapter.ApiTokenPersistenceAdapter;
    import br.com.archbase.security.adapter.SecurityAdapter;
    import br.com.archbase.security.domain.dto.ApiTokenDto;
    import br.com.archbase.security.domain.entity.ApiToken;
    import br.com.archbase.security.persistence.ApiTokenEntity;
    import br.com.archbase.security.persistence.UserEntity;
    import br.com.archbase.security.repository.ApiTokenRepository;
    import br.com.archbase.security.repository.UserJpaRepository;
    import br.com.archbase.security.usecase.ApiTokenUseCase;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;
    import java.util.UUID;

    @Service
    public class ApiTokenService implements ApiTokenUseCase, FindDataWithFilterQuery<String, ApiTokenDto> {

        private static final Logger logger = LoggerFactory.getLogger(ApiTokenService.class);


        @Autowired
        private ApiTokenRepository apiTokenRepository;
        @Autowired
        private UserJpaRepository userRepository;

        @Autowired
        private ApiTokenPersistenceAdapter apiTokenPersistenceAdapter;

        @Autowired
        private SecurityAdapter securityAdapter;

        @Autowired
        private ArchbaseEmailService emailService;

        public boolean activateToken(String token, String tenantId) {
            logger.info("Tentando ativar o token: {} para o tenantId: {}", token, tenantId);

            // Usa QueryDSL via ApiTokenPersistenceAdapter
            Optional<ApiTokenEntity> apiToken = apiTokenPersistenceAdapter.findByTokenAndTenantId(token, tenantId);
            if (apiToken.isPresent()) {
                logger.info("Token encontrado: {}", token);
                if (!apiToken.get().getActivated()) {
                    apiToken.get().setActivated(true);
                    apiTokenRepository.save(apiToken.get());
                    logger.info("Token ativado com sucesso: {}", token);
                    return true;
                } else {
                    logger.warn("Token já está ativado: {}", token);
                }
            } else {
                logger.warn("Token não encontrado: {}", token);
            }
            return false;
        }

        @Override
        public ApiTokenDto findById(String s) {
            return null;
        }

        @Override
        public Page<ApiTokenDto> findAll(int page, int size) {
            return apiTokenPersistenceAdapter.findAll(page, size);
        }

        @Override
        public Page<ApiTokenDto> findAll(int page, int size, String[] sort) {
            return apiTokenPersistenceAdapter.findAll(page, size, sort);
        }

        @Override
        public List<ApiTokenDto> findAll(List<String> strings) {
            return apiTokenPersistenceAdapter.findAll(strings);
        }

        @Override
        public Page<ApiTokenDto> findWithFilter(String filter, int page, int size) {
            return apiTokenPersistenceAdapter.findWithFilter(filter,page,size);
        }

        @Override
        public Page<ApiTokenDto> findWithFilter(String filter, int page, int size, String[] sort) {
            return apiTokenPersistenceAdapter.findWithFilter(filter,page,size,sort);
        }

        @Override
        public ApiTokenDto createToken(String email, LocalDateTime expirationDate, String name, String description) {
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

            String token = UUID.randomUUID().toString();

            ApiTokenEntity apiToken = ApiTokenEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .createdByUser(securityAdapter.getLoggedUser().getUserName())
                    .createEntityDate(LocalDateTime.now())
                    .token(token)
                    .name(name)
                    .description(description)
                    .user(user)
                    .expirationDate(expirationDate)
                    .revoked(false)
                    .activated(false)
                    .build();
            // Persiste ANTES de notificar. Na ordem anterior, o e-mail saía primeiro: sem uma
            // implementação de ArchbaseEmailService o default lança e o token nunca chegava a ser
            // criado (500 na criação); e, mesmo com e-mail configurado, uma falha no save mandava
            // ao usuário um token que não existe.
            ApiTokenDto saved = apiTokenRepository.save(apiToken).toDto();
            try {
                emailService.sendActivationTokenApiEmail(email, token, user.getUsername(), name);
            } catch (RuntimeException e) {
                // Notificar é acessório: quem chamou já recebeu o token na resposta e pode ativá-lo.
                // Falhar aqui destruiria o token recém-criado por causa do canal de aviso.
                logger.warn("Token de API '{}' criado, mas o e-mail de ativação não foi enviado: {}",
                        name, e.getMessage());
            }
            return saved;
        }

        @Override
        public void revokeToken(String token) {
            Optional<ApiTokenEntity> apiToken = apiTokenPersistenceAdapter.findByToken(token);
            if (apiToken.isPresent()){
                apiToken.get().setRevoked(true);
                apiTokenRepository.save(apiToken.get());
                logger.info("Token revogado com sucesso: {}", token);
            } else {
                logger.warn("Token não encontrado para revogação: {}", token);
            }
        }

        @Override
        public boolean validateToken(String token) {
            return apiTokenPersistenceAdapter.validateToken(token);
        }

        /**
         * <b>Transacional de propósito.</b> Quem chama é o filtro de autenticação, que roda na
         * cadeia de servlet — fora do {@code OpenEntityManagerInView}, que só abre no interceptor
         * do MVC. Sem uma transação aqui, o {@code toDomain()} estoura ao tocar as coleções lazy do
         * usuário ({@code groups}) com "no session", e a autenticação por token de API falhava
         * depois de o token já ter sido dado como válido.
         */
        @Override
        @Transactional(readOnly = true)
        public Optional<ApiToken> getApiToken(String token) {
            Optional<ApiTokenEntity> optionalApiTokenEntity = apiTokenPersistenceAdapter.findByToken(token);
            return optionalApiTokenEntity.map(ApiTokenEntity::toDomain);
        }
    }

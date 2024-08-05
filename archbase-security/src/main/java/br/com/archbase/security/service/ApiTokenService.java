    package br.com.archbase.security.service;

    import br.com.archbase.ddd.context.ArchbaseTenantContext;
    import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
    import br.com.archbase.security.adapter.ApiTokenPersistenceAdapter;
    import br.com.archbase.security.adapter.SecurityAdapter;
    import br.com.archbase.security.domain.dto.ApiTokenDto;
    import br.com.archbase.security.domain.entity.ApiToken;
    import br.com.archbase.security.persistence.ApiTokenEntity;
    import br.com.archbase.security.persistence.UserEntity;
    import br.com.archbase.security.repository.ApiTokenNativeRepository;
    import br.com.archbase.security.repository.ApiTokenRepository;
    import br.com.archbase.security.repository.UserJpaRepository;
    import br.com.archbase.security.usecase.ApiTokenUseCase;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.stereotype.Service;

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

        @Autowired
        private ApiTokenNativeRepository apiTokenNativeRepository;

        public boolean activateToken(String token, String tenantId) {
            logger.info("Tentando ativar o token: {} para o tenantId: {}", token, tenantId);

            // Use a consulta nativa
            Optional<ApiTokenEntity> apiToken = apiTokenNativeRepository.findByTokenAndTenantId(token, tenantId);
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
            emailService.sendActivationTokenApiEmail(email, token, user.getUsername(), name);
            return apiTokenRepository.save(apiToken).toDto();
        }

        @Override
        public void revokeToken(String token) {
            Optional<ApiTokenEntity> apiToken = apiTokenRepository.findByToken(token);
            if (apiToken.isPresent()){
                apiToken.get().setRevoked(true);
                apiTokenRepository.save(apiToken.get());
            }
        }

        @Override
        public boolean validateToken(String token) {
            return apiTokenRepository.findByToken(token)
                    .map(t -> !t.getRevoked() && t.getExpirationDate().isAfter(LocalDateTime.now()))
                    .orElse(false);
        }

        @Override
        public Optional<ApiToken> getApiToken(String token) {
            Optional<ApiTokenEntity> optionalApiTokenEntity = apiTokenRepository.findByToken(token);
            return optionalApiTokenEntity.map(ApiTokenEntity::toDomain);
        }
    }

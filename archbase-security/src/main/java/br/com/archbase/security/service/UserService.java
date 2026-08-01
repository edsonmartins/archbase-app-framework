package br.com.archbase.security.service;


import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.security.adapter.SecurityAdapter;
import br.com.archbase.security.adapter.UserPersistenceAdapter;
import br.com.archbase.security.domain.dto.GroupDto;
import br.com.archbase.security.domain.dto.ProfileDto;
import br.com.archbase.security.domain.dto.SimpleUserDto;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.dto.UserGroupDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.password.ArchbasePasswordStrengthPolicy;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.persistence.QGroupEntity;
import br.com.archbase.security.persistence.QProfileEntity;
import br.com.archbase.security.repository.GroupJpaRepository;
import br.com.archbase.security.repository.ProfileJpaRepository;
import br.com.archbase.security.usecase.UserUseCase;
import br.com.archbase.validation.exception.ArchbaseValidationException;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserService implements UserUseCase, FindDataWithFilterQuery<String, UserDto> {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserService.class);

    private final UserPersistenceAdapter persistenceAdapter;
    private final SecurityAdapter securityAdapter;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceListener userServiceListener;
    private final GroupJpaRepository groupJpaRepository;
    private final ProfileJpaRepository profileJpaRepository;
    private final ArchbasePasswordStrengthPolicy passwordStrengthPolicy;

    public UserService(UserPersistenceAdapter persistenceAdapter, SecurityAdapter securityAdapter, PasswordEncoder passwordEncoder, UserServiceListener userServiceListener, GroupJpaRepository groupJpaRepository, ProfileJpaRepository profileJpaRepository, ArchbasePasswordStrengthPolicy passwordStrengthPolicy) {
        this.persistenceAdapter =  persistenceAdapter;
        this.securityAdapter = securityAdapter;
        this.passwordEncoder = passwordEncoder;
        this.userServiceListener = userServiceListener;
        this.groupJpaRepository = groupJpaRepository;
        this.profileJpaRepository = profileJpaRepository;
        this.passwordStrengthPolicy = passwordStrengthPolicy;
    }

    @Override
    public UserDto findById(String id) {
        return persistenceAdapter.findById(id);
    }

    @Override
    public Page<UserDto> findAll(int page, int size) {
        return persistenceAdapter.findAll(page, size);
    }

    @Override
    public Page<UserDto> findAll(int page, int size, String[] sort) {
        return persistenceAdapter.findAll(page, size, sort);
    }

    @Override
    public Optional<User> getUserByEmail(String email)  {
        return persistenceAdapter.getUserByEmail(email);
    }
    @Override
    public List<UserDto> findAll(List<String> ids) {
        return persistenceAdapter.findAll(ids);
    }

    @Override
    public Page<UserDto> findWithFilter(String filter, int page, int size) {
        return persistenceAdapter.findWithFilter(filter, page, size);
    }

    @Override
    public Page<UserDto> findWithFilter(String filter, int page, int size, String[] sort) {
        return persistenceAdapter.findWithFilter(filter, page, size, sort);
    }

    @Override
    public List<UserDto> getAllUsersByGroup(String groupId) {
        return persistenceAdapter.getAllUsersByGroup(groupId);
    }

    @Override
    public Optional<UserDto> findGroupById(String id) {
        return Optional.empty();
    }

    /**
     * Impede que um usuário sem privilégio administrativo crie ou promova um administrador.
     *
     * <p>{@code isAdministrator} chega pelo corpo da requisição e vale como bypass total em
     * {@link ArchbaseSecurityService#hasPermission}. Sem esta checagem, qualquer autenticado que
     * alcançasse {@code POST /api/v1/user} virava administrador enviando um campo — a escalação
     * mais curta do sistema.
     *
     * <p>Vale sempre, independente de
     * {@code archbase.security.admin-endpoints.policy}: a política controla <i>quem chega</i> ao
     * endpoint, esta trava controla <i>o que pode ser concedido</i>, e a segunda não deve depender
     * da primeira estar ligada.
     */
    private void denyAdministratorPromotionByNonAdmin(UserDto userDto) {
        if (!Boolean.TRUE.equals(userDto.getIsAdministrator())) {
            return;
        }
        if (isRequestFromUnverifiablePrincipal()) {
            // Há alguém autenticado, mas o principal não é um UserEntity — típico de aplicação com
            // UserDetailsService próprio. Não dá para afirmar que é administrador, e "não consegui
            // verificar" não pode dar no mesmo resultado que "verifiquei e é admin": seria a
            // escalação que esta trava existe para fechar, reaberta por configuração da aplicação.
            throw new ArchbaseValidationException(
                    "Não foi possível verificar o privilégio de administrador do solicitante.");
        }

        User loggedUser = securityAdapter.getLoggedUserOrNull();
        if (loggedUser == null) {
            // Ninguém autenticado: chamada interna (bootstrap, seed, importação), fora de requisição
            // HTTP. Não há a quem negar — mas fica registrado, porque é o caminho pelo qual um admin
            // nasce sem revisão.
            logger.warn("Criação/alteração de usuário administrador sem usuário autenticado no contexto");
            return;
        }
        if (!Boolean.TRUE.equals(loggedUser.getIsAdministrator())) {
            throw new ArchbaseValidationException(
                    "Apenas um administrador pode conceder privilégio de administrador.");
        }
    }

    /**
     * Verdadeiro quando existe autenticação no contexto mas o principal não é um {@link UserEntity},
     * caso em que {@code getLoggedUserOrNull()} devolve {@code null} por não conseguir resolver — e
     * não por ausência de usuário.
     */
    private boolean isRequestFromUnverifiablePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !(authentication.getPrincipal() instanceof UserEntity);
    }

    /**
     * Impede que um não-administrador altere a conta de um administrador — inclusive a senha, o que
     * seria tomada de conta direta.
     */
    private void denyEditingAdministratorByNonAdmin(UserDto currentUserDto) {
        if (!Boolean.TRUE.equals(currentUserDto.getIsAdministrator())) {
            return;
        }
        if (isRequestFromUnverifiablePrincipal()) {
            throw new ArchbaseValidationException(
                    "Não foi possível verificar o privilégio de administrador do solicitante.");
        }
        User loggedUser = securityAdapter.getLoggedUserOrNull();
        if (loggedUser == null) {
            return;
        }
        if (Boolean.TRUE.equals(loggedUser.getIsAdministrator())) {
            return;
        }
        // O próprio administrador editando a si mesmo é permitido. getId() pode ser nulo em
        // principal montado à mão, e comparar a partir dele estouraria NPE — então a igualdade
        // parte do id atual, que veio do banco.
        boolean editandoASiMesmo = loggedUser.getId() != null
                && currentUserDto.getId() != null
                && currentUserDto.getId().equals(loggedUser.getId().toString());
        if (!editandoASiMesmo) {
            throw new ArchbaseValidationException(
                    "Apenas um administrador pode alterar a conta de outro administrador.");
        }
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        UserDto originalUserDto = new UserDto();
        BeanUtils.copyProperties(userDto, originalUserDto);
        Optional<User> usuarioOptional = persistenceAdapter.getUserByEmail(userDto.getEmail());
        if (usuarioOptional.isPresent()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s já cadastrado.",userDto.getEmail()));
        }
        denyAdministratorPromotionByNonAdmin(userDto);
        // Só valida quando há senha. Criação sem senha é legítima (convite, SSO, provisionamento
        // automático de login social); exigir força aí faria esses fluxos quebrarem no dia em que
        // um operador ligasse a política — e só em produção, já que ela vem desligada.
        if (!StringUtils.isBlank(userDto.getPassword())) {
            passwordStrengthPolicy.validate(userDto.getPassword());
        }
        userServiceListener.onBeforeCreate(originalUserDto);
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        UserDto user = persistenceAdapter.createUser(userDto);
        userServiceListener.onAfterCreate(originalUserDto,user);
        return user;
    }

    @Override
    @Transactional
    public Optional<UserDto> updateUser(String id, UserDto userDto) {
        UserDto originalUserDto = new UserDto();
        BeanUtils.copyProperties(userDto, originalUserDto);
        Optional<User> usuarioOptional = persistenceAdapter.getUserByEmail(userDto.getEmail());
        if (usuarioOptional.isPresent() && !usuarioOptional.get().getId().toString().equals(id)) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s já cadastrado.",userDto.getEmail()));
        }
        UserDto currentUserDto = findById(id);
        if (currentUserDto==null){
            throw new ArchbaseValidationException("Usuário não encontrado.");
        }
        denyAdministratorPromotionByNonAdmin(userDto);
        denyEditingAdministratorByNonAdmin(currentUserDto);
        userServiceListener.onBeforeUpdate(originalUserDto);
        if (!StringUtils.isBlank(userDto.getPassword())) {
            passwordStrengthPolicy.validate(userDto.getPassword());
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        Optional<UserDto> result = persistenceAdapter.updateUser(id, userDto);
        userServiceListener.onAfterUpdate(originalUserDto, currentUserDto, result.get());
        return result;
    }

    @Override
    @Transactional
    public String createSimpleUser(SimpleUserDto simpleUserDto) {
        UserDto userDto = convertSimpleUserToUserDto(simpleUserDto);
        UserDto createdUser = createUser(userDto);
        return createdUser.getId();
    }

    @Override
    @Transactional
    public String updateSimpleUser(SimpleUserDto simpleUserDto) {
        if (simpleUserDto.getEmail() == null || simpleUserDto.getEmail().isBlank()) {
            throw new ArchbaseValidationException("O campo 'email' é obrigatório para atualização de usuário");
        }

        Optional<User> userOptional = persistenceAdapter.getUserByEmail(simpleUserDto.getEmail());
        if (userOptional.isEmpty()) {
            throw new ArchbaseValidationException(
                String.format("Usuário com email %s não encontrado", simpleUserDto.getEmail())
            );
        }

        User currentUserEntity = userOptional.get();
        String userId = currentUserEntity.getId().toString();

        UserDto currentUser = UserDto.fromDomain(currentUserEntity);
        UserDto updateData = convertSimpleUserToUserDto(simpleUserDto);

        if (updateData.getName() != null) currentUser.setName(updateData.getName());
        if (updateData.getNickname() != null) currentUser.setNickname(updateData.getNickname());
        if (updateData.getDescription() != null) currentUser.setDescription(updateData.getDescription());
        if (updateData.getPassword() != null && !updateData.getPassword().isBlank()) currentUser.setPassword(updateData.getPassword());
        if (updateData.getChangePasswordOnNextLogin() != null) currentUser.setChangePasswordOnNextLogin(updateData.getChangePasswordOnNextLogin());
        if (updateData.getAllowPasswordChange() != null) currentUser.setAllowPasswordChange(updateData.getAllowPasswordChange());
        if (updateData.getAllowMultipleLogins() != null) currentUser.setAllowMultipleLogins(updateData.getAllowMultipleLogins());
        if (updateData.getPasswordNeverExpires() != null) currentUser.setPasswordNeverExpires(updateData.getPasswordNeverExpires());
        if (updateData.getAccountDeactivated() != null) currentUser.setAccountDeactivated(updateData.getAccountDeactivated());
        if (updateData.getAccountLocked() != null) currentUser.setAccountLocked(updateData.getAccountLocked());
        if (updateData.getUnlimitedAccessHours() != null) currentUser.setUnlimitedAccessHours(updateData.getUnlimitedAccessHours());
        if (updateData.getIsAdministrator() != null) currentUser.setIsAdministrator(updateData.getIsAdministrator());
        if (updateData.getProfile() != null) currentUser.setProfile(updateData.getProfile());
        if (updateData.getGroups() != null && !updateData.getGroups().isEmpty()) currentUser.setGroups(updateData.getGroups());

        Optional<UserDto> updatedUser = updateUser(userId, currentUser);
        return updatedUser.orElseThrow(() ->
            new ArchbaseValidationException("Falha ao atualizar usuário")).getId();
    }

    private UserDto convertSimpleUserToUserDto(SimpleUserDto simpleDto) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(simpleDto, userDto);
        userDto.setUserName(simpleDto.getEmail());

        if (simpleDto.getProfile() != null && !simpleDto.getProfile().isBlank()) {
            QProfileEntity qProfile = QProfileEntity.profileEntity;
            BooleanExpression predicate = qProfile.name.eq(simpleDto.getProfile());
            List<br.com.archbase.security.persistence.ProfileEntity> profiles = profileJpaRepository.findAll(predicate);
            if (profiles.isEmpty()) {
                throw new ArchbaseValidationException(
                    String.format("Perfil '%s' não encontrado", simpleDto.getProfile())
                );
            }
            ProfileDto profile = ProfileDto.fromDomain(profiles.get(0).toDomain());
            userDto.setProfile(profile);
        }

        if (simpleDto.getGroups() != null && !simpleDto.getGroups().isEmpty()) {
            List<String> groupNames = simpleDto.getGroups().stream()
                .filter(name -> name != null && !name.isBlank())
                .toList();

            if (!groupNames.isEmpty()) {
                QGroupEntity qGroup = QGroupEntity.groupEntity;
                BooleanExpression predicate = qGroup.name.in(groupNames);
                List<GroupDto> groups = groupJpaRepository.findAll(predicate)
                    .stream()
                    .map(groupEntity -> GroupDto.fromDomain(groupEntity.toDomain()))
                    .toList();

                if (groups.size() != groupNames.size()) {
                    Set<String> foundNames = groups.stream()
                        .map(GroupDto::getName)
                        .collect(Collectors.toSet());
                    List<String> missingNames = groupNames.stream()
                        .filter(name -> !foundNames.contains(name))
                        .collect(Collectors.toList());
                    throw new ArchbaseValidationException(
                        String.format("Grupos não encontrados: %s", String.join(", ", missingNames))
                    );
                }

                List<UserGroupDto> userGroups = groups.stream()
                    .map(group -> UserGroupDto.builder().group(group).build())
                    .collect(Collectors.toList());
                userDto.setGroups(userGroups);
            }
        }

        return userDto;
    }

    @Override
    public void removeUser(String id) {
        UserDto userDto = findById(id);
        if (userDto==null){
            throw new ArchbaseValidationException("Usuário não encontrada.");
        }
        userServiceListener.onBeforeRemove(userDto);
        persistenceAdapter.removerUser(id);
        userServiceListener.onAfterRemove(userDto);
    }

    @Override
    public void addPermission(String userId, String actionId) {
        persistenceAdapter.addPermission(userId, actionId);
    }

    @Override
    public void removePermission(String permissionId) {
        persistenceAdapter.removePermission(permissionId);
    }

    @Override
    public Optional<User> getLoggedUser() {
        return Optional.ofNullable(securityAdapter.getLoggedUser());
    }
}

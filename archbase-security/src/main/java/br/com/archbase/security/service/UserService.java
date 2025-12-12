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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserService implements UserUseCase, FindDataWithFilterQuery<String, UserDto> {

    private final UserPersistenceAdapter persistenceAdapter;
    private final SecurityAdapter securityAdapter;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceListener userServiceListener;
    private final GroupJpaRepository groupJpaRepository;
    private final ProfileJpaRepository profileJpaRepository;

    public UserService(UserPersistenceAdapter persistenceAdapter, SecurityAdapter securityAdapter, PasswordEncoder passwordEncoder, UserServiceListener userServiceListener, GroupJpaRepository groupJpaRepository, ProfileJpaRepository profileJpaRepository) {
        this.persistenceAdapter =  persistenceAdapter;
        this.securityAdapter = securityAdapter;
        this.passwordEncoder = passwordEncoder;
        this.userServiceListener = userServiceListener;
        this.groupJpaRepository = groupJpaRepository;
        this.profileJpaRepository = profileJpaRepository;
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
        Optional<User> usuarioOptional = persistenceAdapter.getUserByEmail(email);
        if(usuarioOptional.isEmpty()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s não foi encontrado.",email));
        }
        return usuarioOptional;
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

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        UserDto originalUserDto = new UserDto();
        BeanUtils.copyProperties(userDto, originalUserDto);
        Optional<User> usuarioOptional = persistenceAdapter.getUserByEmail(userDto.getEmail());
        if (usuarioOptional.isPresent()) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s já cadastrado.",userDto.getEmail()));
        }
        userServiceListener.onBeforeCreate(originalUserDto);
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        UserDto user = persistenceAdapter.createUser(userDto);
        userServiceListener.onAfterCreate(originalUserDto,user);
        return user;
    }

    @Override
    @Transactional
    public String createSimpleUser(SimpleUserDto simpleUserDto) {
        // Convert SimpleUserDto to UserDto with database lookups
        UserDto userDto = convertSimpleUserToUserDto(simpleUserDto);

        // Delegate to existing createUser method (handles validation, hooks, password encoding)
        UserDto createdUser = createUser(userDto);

        // Return the created user's ID
        return createdUser.getId();
    }

    @Override
    @Transactional
    public String updateSimpleUser(String id, SimpleUserDto simpleUserDto) {
        // 1. Fetch current user data
        UserDto currentUser = findById(id);
        if (currentUser == null) {
            throw new ArchbaseValidationException(String.format("Usuário com ID %s não encontrado", id));
        }

        // 2. Convert SimpleUserDto to UserDto (handles profile and groups lookup)
        UserDto updateData = convertSimpleUserToUserDto(simpleUserDto);

        // 3. Merge only non-null fields from updateData into currentUser
        if (updateData.getName() != null) {
            currentUser.setName(updateData.getName());
        }
        if (updateData.getNickname() != null) {
            currentUser.setNickname(updateData.getNickname());
        }
        if (updateData.getDescription() != null) {
            currentUser.setDescription(updateData.getDescription());
        }
        if (updateData.getPassword() != null && !updateData.getPassword().isBlank()) {
            currentUser.setPassword(updateData.getPassword());
        }
        if (updateData.getChangePasswordOnNextLogin() != null) {
            currentUser.setChangePasswordOnNextLogin(updateData.getChangePasswordOnNextLogin());
        }
        if (updateData.getAllowPasswordChange() != null) {
            currentUser.setAllowPasswordChange(updateData.getAllowPasswordChange());
        }
        if (updateData.getAllowMultipleLogins() != null) {
            currentUser.setAllowMultipleLogins(updateData.getAllowMultipleLogins());
        }
        if (updateData.getPasswordNeverExpires() != null) {
            currentUser.setPasswordNeverExpires(updateData.getPasswordNeverExpires());
        }
        if (updateData.getAccountDeactivated() != null) {
            currentUser.setAccountDeactivated(updateData.getAccountDeactivated());
        }
        if (updateData.getAccountLocked() != null) {
            currentUser.setAccountLocked(updateData.getAccountLocked());
        }
        if (updateData.getUnlimitedAccessHours() != null) {
            currentUser.setUnlimitedAccessHours(updateData.getUnlimitedAccessHours());
        }
        if (updateData.getIsAdministrator() != null) {
            currentUser.setIsAdministrator(updateData.getIsAdministrator());
        }

        // Profile was already converted to ProfileDto by convertSimpleUserToUserDto
        if (updateData.getProfile() != null) {
            currentUser.setProfile(updateData.getProfile());
        }

        // Groups were already converted to UserGroupDto list by convertSimpleUserToUserDto
        if (updateData.getGroups() != null && !updateData.getGroups().isEmpty()) {
            currentUser.setGroups(updateData.getGroups());
        }

        // 4. Delegate to existing updateUser method (handles validation, hooks, password encoding)
        Optional<UserDto> updatedUser = updateUser(id, currentUser);

        // 5. Return the updated user's ID
        return updatedUser.orElseThrow(() ->
            new ArchbaseValidationException("Falha ao atualizar usuário")).getId();
    }

    private UserDto convertSimpleUserToUserDto(SimpleUserDto simpleDto) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(simpleDto, userDto);
        userDto.setUserName(simpleDto.getEmail());

        // Lookup profile by name
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

        // Lookup groups by names
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

                // Validate all groups were found
                if (groups.size() != groupNames.size()) {
                    Set<String> foundNames = groups.stream()
                        .map(GroupDto::getName)
                        .collect(Collectors.toSet());
                    List<String> missingNames = groupNames.stream()
                        .filter(name -> !foundNames.contains(name))
                        .collect(Collectors.toList());
                    throw new ArchbaseValidationException(
                        String.format("Grupos não encontrados: %s",
                            String.join(", ", missingNames))
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
    @Transactional
    public Optional<UserDto> updateUser(String id, UserDto userDto) {
        UserDto originalUserDto = new UserDto();
        BeanUtils.copyProperties(userDto, originalUserDto);
        Optional<User> usuarioOptional = persistenceAdapter.getUserByEmail(userDto.getEmail());
        if (usuarioOptional.isPresent() && !usuarioOptional.get().getId().toString().equals(id)) {
            throw new ArchbaseValidationException(String.format("Usuário com email %s já cadastrado.",userDto.getEmail()));
        }
        userServiceListener.onBeforeUpdate(originalUserDto);
        if (!StringUtils.isBlank(userDto.getPassword())) {
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        UserDto currentUserDto = findById(id);
        if (currentUserDto==null){
            throw new ArchbaseValidationException("Usuário não encontrado.");
        }
        Optional<UserDto> result = persistenceAdapter.updateUser(id, userDto);
        userServiceListener.onAfterUpdate(originalUserDto, currentUserDto, result.get());
        return result;
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

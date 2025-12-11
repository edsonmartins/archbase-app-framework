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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
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

    private UserDto convertSimpleUserToUserDto(
            SimpleUserDto simpleDto,
            Map<String, ProfileDto> profilesByName,
            Map<String, GroupDto> groupsByName
    ) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(simpleDto, userDto);

        // Converter profile (String -> ProfileDto)
        if (simpleDto.getProfile() != null && !simpleDto.getProfile().isBlank()) {
            ProfileDto profile = profilesByName.get(simpleDto.getProfile());
            if (profile == null) {
                throw new ArchbaseValidationException(
                        String.format("Perfil '%s' não encontrado para usuário %s",
                                simpleDto.getProfile(), simpleDto.getEmail())
                );
            }
            userDto.setProfile(profile);
        }

        // Converter groups (List<String> -> List<UserGroupDto>)
        if (simpleDto.getGroups() != null && !simpleDto.getGroups().isEmpty()) {
            List<UserGroupDto> userGroups = simpleDto.getGroups().stream()
                    .filter(groupName -> groupName != null && !groupName.isBlank())
                    .map(groupName -> {
                        GroupDto group = groupsByName.get(groupName);
                        if (group == null) {
                            throw new ArchbaseValidationException(
                                    String.format("Grupo '%s' não encontrado para usuário %s",
                                            groupName, simpleDto.getEmail())
                            );
                        }
                        return UserGroupDto.builder()
                                .group(group)
                                .build();
                    })
                    .collect(Collectors.toList());
            userDto.setGroups(userGroups);
        }

        return userDto;
    }

    @Override
    @Transactional
    public List<String> createUsers(List<SimpleUserDto> usersDtos) {
        // 1. Buscar usuários existentes por email (idempotência)
        List<User> existingUsers = persistenceAdapter.getUsersByEmails(
                usersDtos.stream().map(SimpleUserDto::getEmail).toList()
        );

        // Criar mapa de emails existentes -> IDs
        Map<String, String> existingUserIdsByEmail = existingUsers.stream()
                .collect(Collectors.toMap(User::getEmail, user -> user.getId().toString()));

        // 2. Extrair nomes únicos de perfis e grupos
        Set<String> uniqueProfileNames = usersDtos.stream()
                .map(SimpleUserDto::getProfile)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());

        Set<String> uniqueGroupNames = usersDtos.stream()
                .flatMap(dto -> dto.getGroups().stream())
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());

        // 3. Buscar profiles em lote com QueryDSL
        Map<String, ProfileDto> profilesByName = new HashMap<>();
        if (!uniqueProfileNames.isEmpty()) {
            QProfileEntity qProfile = QProfileEntity.profileEntity;
            BooleanExpression predicate = qProfile.name.in(uniqueProfileNames);
            List<ProfileDto> profiles = profileJpaRepository.findAll(predicate)
                    .stream()
                    .map(profileEntity -> ProfileDto.fromDomain(profileEntity.toDomain()))
                    .toList();
            profilesByName = profiles.stream()
                    .collect(Collectors.toMap(ProfileDto::getName, Function.identity()));
        }

        // 4. Buscar groups em lote com QueryDSL
        Map<String, GroupDto> groupsByName = new HashMap<>();
        if (!uniqueGroupNames.isEmpty()) {
            QGroupEntity qGroup = QGroupEntity.groupEntity;
            BooleanExpression predicate = qGroup.name.in(uniqueGroupNames);
            List<GroupDto> groups = groupJpaRepository.findAll(predicate)
                    .stream()
                    .map(groupEntity -> GroupDto.fromDomain(groupEntity.toDomain()))
                    .toList();
            groupsByName = groups.stream()
                    .collect(Collectors.toMap(GroupDto::getName, Function.identity()));
        }

        // 5. Processar cada usuário sequencialmente
        List<UserDto> usersToCreate = new ArrayList<>();
        List<UserDto> originalUserDtos = new ArrayList<>();

        for (SimpleUserDto simpleDto : usersDtos) {
            // Pula se usuário já existe (idempotência)
            if (existingUserIdsByEmail.containsKey(simpleDto.getEmail())) {
                continue;
            }

            // Converter para UserDto usando caches
            // NOTA: Se profile/group não encontrado, lança exceção → rollback
            UserDto userDto = convertSimpleUserToUserDto(simpleDto, profilesByName, groupsByName);

            // Criar cópia original para hooks
            UserDto originalUserDto = new UserDto();
            BeanUtils.copyProperties(userDto, originalUserDto);

            // Hook onBeforeCreate
            // NOTA: Se falhar, exceção propaga → rollback completo
            userServiceListener.onBeforeCreate(originalUserDto);

            // Codificar senha
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));

            // Adicionar às listas
            usersToCreate.add(userDto);
            originalUserDtos.add(originalUserDto);
        }

        // 6. Salvar todos em lote
        Map<String, String> newUserIdsByEmail = new HashMap<>();

        if (!usersToCreate.isEmpty()) {
            List<UserDto> savedUsers = persistenceAdapter.createUsers(usersToCreate);

            // Hook onAfterCreate para cada
            // NOTA: Se falhar, exceção propaga → rollback completo
            for (int i = 0; i < savedUsers.size(); i++) {
                userServiceListener.onAfterCreate(
                        originalUserDtos.get(i),
                        savedUsers.get(i)
                );
                newUserIdsByEmail.put(savedUsers.get(i).getEmail(), savedUsers.get(i).getId());
            }
        }

        // 7. Retornar IDs na ordem original do request
        // Combinar IDs existentes + novos, mantendo ordem do request
        List<String> resultIds = new ArrayList<>();
        for (SimpleUserDto dto : usersDtos) {
            String userId = existingUserIdsByEmail.get(dto.getEmail());
            if (userId == null) {
                userId = newUserIdsByEmail.get(dto.getEmail());
            }
            resultIds.add(userId);
        }

        return resultIds;
    }
}

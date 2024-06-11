package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.UserPersistencePort;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.exception.ArchbaseSecurityException;
import br.com.archbase.security.persistence.*;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.PermissionJpaRepository;
import br.com.archbase.security.repository.UserJpaRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserPersistenceAdapter implements UserPersistencePort, FindDataWithFilterQuery<String, UserDto> {

    @Autowired
    private UserJpaRepository repository;
    @Autowired
    private ActionJpaRepository actionJpaRepository;

    @Autowired
    private PermissionJpaRepository permissionJpaRepository;

    @Autowired
    private SecurityAdapter securityAdapter;


    @Override
    public UserDto findById(String id) {
        Optional<UserEntity> byId = repository.findById(id);
        return byId.map(UserEntity::toDto).orElse(null);
    }

    @Override
    public Page<UserDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> result = repository.findAll(pageable);
        List<UserDto> list = result.stream().map(UserEntity::toDto).toList();
        return new PageUser(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<UserDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<UserEntity> result = repository.findAll(pageable);
        List<UserDto> list = result.stream().map(UserEntity::toDto).toList();
        return new PageUser(list, pageable, result.getTotalElements());
    }

    @Override
    public List<UserDto> findAll(List<String> ids) {
        List<UserEntity> result = repository.findAllById(ids);
        return result.stream().map(UserEntity::toDto).toList();
    }

    @Override
    public Page<UserDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> result = repository.findAll(filter, pageable);
        List<UserDto> list = result.stream().map(UserEntity::toDto).toList();
        return new PageUser(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<UserDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<UserEntity> result = repository.findAll(filter, pageable);
        List<UserDto> list = result.stream().map(UserEntity::toDto).toList();
        return new PageUser(list, pageable, result.getTotalElements());
    }

    @Override
    public UserDto createUser(UserDto userDto)  {
        return repository.save(UserEntity.fromDomain(userDto.toDomain())).toDto();
    }

    @Override
    public Optional<UserDto> updateUser(String id, UserDto userDto) {
        User loggedUser = securityAdapter.getLoggedUser();
        return Optional.of(repository.findById(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(existingEntity -> {
                    existingEntity.setName(userDto.getName());
                    existingEntity.setDescription(userDto.getDescription());
                    existingEntity.setCode(userDto.getCode());
                    existingEntity.setPassword(userDto.getPassword());
                    existingEntity.setAvatar(userDto.getAvatar());
                    existingEntity.setAccountDeactivated(userDto.getAccountDeactivated());
                    existingEntity.setAccountLocked(userDto.getAccountLocked());
                    existingEntity.setAllowMultipleLogins(userDto.getAllowMultipleLogins());
                    existingEntity.setAllowPasswordChange(userDto.getAllowPasswordChange());
                    existingEntity.setChangePasswordOnNextLogin(userDto.getChangePasswordOnNextLogin());
                    existingEntity.setIsAdministrator(userDto.getIsAdministrator());
                    existingEntity.setPasswordNeverExpires(userDto.getPasswordNeverExpires());
                    existingEntity.setUnlimitedAccessHours(userDto.getUnlimitedAccessHours());
                    existingEntity.setUserName(userDto.getUserName());
                    existingEntity.setEmail(userDto.getEmail());
                    existingEntity.setUpdateEntityDate(LocalDateTime.now());
                    existingEntity.setLastModifiedByUser(loggedUser.getUserName());
                    existingEntity.setProfile(userDto.getProfile() != null ? ProfileEntity.fromDomain(userDto.getProfile().toDomain()):null);
                    existingEntity.setAccessSchedule(userDto.getAccessSchedule() != null ? AccessScheduleEntity.fromDomain(userDto.getAccessSchedule().toDomain()):null);
                    return repository.save(existingEntity).toDto();
                });
    }

    @Override
    public void removerUser(String id) {
        repository.deleteById(id);
    }

    @Override
    public Optional<User> getUserById(String id)  {
        Optional<UserEntity> byId = repository.findById(id);
        return byId.map(UserEntity::toDomain);
    }

    @Override
    public List<UserDto> getAllUsersByGroup(String groupId) {
        QUserEntity qUser = QUserEntity.userEntity;
        QGroupEntity qGroup = QGroupEntity.groupEntity;

        // Construindo o predicado para QueryDSL
        BooleanExpression predicate = qUser.groups.any().id.eq(groupId);

        List<UserEntity> users = (List<UserEntity>) repository.findAll(predicate);

        return users.stream()
                .map(UserEntity::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> getUserByEmail(String email)  {
        Optional<UserEntity> optionalUser = repository.findByEmail(email);
        return optionalUser.map(UserEntity::toDomain);
    }

    @Override
    public boolean existeUserByEmail(String email)  {
        return repository.existsByEmail(email);
    }

    @Override
    public void addPermission(String userId, String actionId) {
        UserEntity user = repository.findById(userId).orElseThrow(() -> new ArchbaseSecurityException("Usuário %s não encontrado".formatted(userId)));
        ActionEntity action = actionJpaRepository.findById(actionId).orElseThrow(() -> new ArchbaseSecurityException("Ação %s não encontrada".formatted(actionId)));

        PermissionEntity permission = new PermissionEntity();
        permission.setAction(action);
        permission.setSecurity(user);
        permissionJpaRepository.save(permission);
    }

    @Override
    public void removePermission(String permissionId) {
        PermissionEntity permission = permissionJpaRepository.findById(permissionId).orElseThrow(() -> new ArchbaseSecurityException("Permissão %s não encontrada.".formatted(permissionId)));
        permissionJpaRepository.delete(permission);
    }

    static class PageUser extends PageImpl<UserDto> {
        public PageUser(List<UserDto> content) {
            super(content);
        }

        public PageUser(List<UserDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListUser extends ArrayList<UserDto> {
        public ListUser(Collection<? extends UserDto> c) {
            super(c);
        }
    }
}

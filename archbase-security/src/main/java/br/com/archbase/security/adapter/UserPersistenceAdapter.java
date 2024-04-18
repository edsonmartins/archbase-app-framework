package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.domain.dto.UserDto;
import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.mapper.UserPersistenceMapper;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserPersistencePort, FindDataWithFilterQuery<String, UserDto> {

    private final UserJpaRepository repository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

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
    public User saveUser(User user)  {
        return null;
    }

    @Override
    public Optional<User> getUserById(String id)  {
        Optional<UserEntity> byId = repository.findById(id);
        return byId.map(UserEntity::toDomain);
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

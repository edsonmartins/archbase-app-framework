package br.com.archbase.security.mapper.impl;

import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.mapper.UserPersistenceMapper;
import br.com.archbase.security.persistence.UserEntity;
import org.springframework.stereotype.Component;

@Component("userPersistenceMapper")
public class UserPersistenceMapperImpl implements UserPersistenceMapper {
    @Override
    public UserEntity toUserEntity(User user) {
        return UserEntity.fromDomain(user);
    }

    @Override
    public User toUser(UserEntity userEntity) {
        return userEntity.toDomain();
    }
}

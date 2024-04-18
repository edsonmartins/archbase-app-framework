package br.com.archbase.security.mapper;

import br.com.archbase.security.domain.entity.User;
import br.com.archbase.security.persistence.UserEntity;


public interface UserPersistenceMapper {

    UserEntity toUserEntity(User user);

    User toUser(UserEntity userEntity);

}
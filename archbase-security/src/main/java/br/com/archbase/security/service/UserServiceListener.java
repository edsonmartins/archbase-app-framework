package br.com.archbase.security.service;

import br.com.archbase.security.domain.dto.UserDto;

public interface UserServiceListener {

    public void onBeforeCreate(UserDto receivedUser);
    public void onAfterCreate(UserDto receivedUser, UserDto savedUser);
    public void onBeforeUpdate(UserDto receivedUser);
    public void onAfterUpdate(UserDto receivedUser, UserDto savedUser);
    public void onBeforeRemove(UserDto removedUser);
    public void onAfterRemove(UserDto removedUser);


}

package br.com.archbase.security.service;

import br.com.archbase.security.domain.dto.UserDto;


public class DefaultUserServiceListener implements UserServiceListener {

    @Override
    public void onBeforeCreate(UserDto receivedUser) {

    }

    @Override
    public void onAfterCreate(UserDto receivedUser, UserDto savedUser) {

    }

    @Override
    public void onBeforeUpdate(UserDto receivedUser) {

    }

    @Override
    public void onAfterUpdate(UserDto receivedUser, UserDto savedUser) {

    }

    @Override
    public void onBeforeRemove(UserDto removedUser) {

    }

    @Override
    public void onAfterRemove(UserDto removedUser) {

    }
}

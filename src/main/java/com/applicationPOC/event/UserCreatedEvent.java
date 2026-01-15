package com.applicationPOC.event;

import org.springframework.context.ApplicationEvent;

import com.applicationPOC.model.UserDto;

public class UserCreatedEvent extends ApplicationEvent {
    private final UserDto user;

    public UserCreatedEvent(Object source, UserDto user) {
        super(source);
        this.user = user;
    }

    public UserDto getUser() {
        return user;
    }
}

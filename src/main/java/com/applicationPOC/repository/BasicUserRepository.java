package com.applicationPOC.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.applicationPOC.model.UserDto;

@Repository
public class BasicUserRepository {
	
	private static Map<Long, UserDto> userStore = new HashMap<>();
	private static long idCounter = 1;

	public UserDto save(UserDto user) {
		if (user.getId() == null) {
			user.setId(idCounter++);
		}
		userStore.put(user.getId(), user);
		return user;
	}

	public Optional<UserDto> findById(Long id) {
		return Optional.ofNullable(userStore.get(id));
	}

}

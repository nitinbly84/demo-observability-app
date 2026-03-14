package com.applicationPOC.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.applicationPOC.model.User;
import com.applicationPOC.repository.UserRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private Logger logger = LoggerFactory.getLogger(DatabaseUserDetailsService.class);

	public DatabaseUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		logger.info("Loading user by username: {}", username);
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		if (!user.isEnabled()) {
			throw new UsernameNotFoundException("User is disabled: " + username);
		}

		return new MyUserDetails(user);
	}
}


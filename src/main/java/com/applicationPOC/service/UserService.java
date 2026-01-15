package com.applicationPOC.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.applicationPOC.model.User;
import com.applicationPOC.repository.UserRepository;

@Service
public class UserService {

 private final UserRepository userRepository;
 private final PasswordEncoder passwordEncoder;

 public UserService(UserRepository userRepository,
                    PasswordEncoder passwordEncoder) {
     this.userRepository = userRepository;
     this.passwordEncoder = passwordEncoder;
 }

 @Transactional
 public User createUser(String username, String rawPassword, String role) {
     String encodedPassword = passwordEncoder.encode(rawPassword);

     User user = new User(username, encodedPassword, role);
     user.setEnabled(true);

     return userRepository.save(user);
 }
}


package com.applicationPOC.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.applicationPOC.event.UserCreatedEvent;
import com.applicationPOC.model.UserDto;
import com.applicationPOC.repository.BasicUserRepository;

@Service
public class DemoService {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private BasicUserRepository userRepository;
	private final ApplicationEventPublisher publisher;
	private static long userIdCounter = 1;

	public DemoService(BasicUserRepository userRepository, ApplicationEventPublisher publisher) {
		this.userRepository = userRepository;
		this.publisher = publisher;
	}

	@Cacheable(cacheNames = "demoCacheDev", key = "#id")
	public String expensiveCall(String id) {
		log.info("Executing expensiveCall for {}", id);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ignored) {
		}
		return "data-for-" + id;
	}

	@Async
	public CompletableFuture<String> asyncOperation(String input) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ignored) {
			}
			return "async-result-" + input;
		});
	}
	
	@Async("transcodingPoolTaskExecutor")
	public CompletableFuture<String> asyncCustomOperation(String input) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ignored) {
			}
			return "custom-async-result-" + input;
		});
	}

	@Transactional
	public UserDto saveUser(UserDto user) {
		// Simulate some processing logic
		log.info("Saving user: {}", user.getName());
		user.setId(userIdCounter++);
		user = userRepository.save(user);
		//user.setName(user.getName().toUpperCase());
		publisher.publishEvent(new UserCreatedEvent(this, user));
		
		// Simulating an error to demonstrate transaction rollback
//		throw new RuntimeException("Simulated exception to demonstrate rollback");
		return user;
	}

	public Optional<UserDto> getUserById(Long id) {
		return userRepository.findById(id);
	}
}


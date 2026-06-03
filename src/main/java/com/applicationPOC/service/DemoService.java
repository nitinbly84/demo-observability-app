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
		// Simulate an expensive operation by sleeping for 2 seconds, nothing else
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ignored) {
		}
		return "data-for-" + id;
	}

	@Async
	public CompletableFuture<String> asyncOperation(String input) {
		/*
		 * Executing asyncOperation on Thread[#107,async-exec-1,5,main] | Is Virtual Thread? false
		 * 
		 * If virtual threads are not enabled, the async operation will run on a regular thread from the common ForkJoinPool,
		 * which is what we see in the output above. 'main' indicates that it's a regular thread group, and 'async-exec-1' is the
		 * name of the thread assigned by Spring's @Async mechanism.
		 * 
		 * Executing asyncOperation on Thread[#118,async-exec-1,5,VirtualThreads] | Is Virtual Thread? false
		 * 
		 * If virtual threads are enabled, the async operation will run on a virtual thread. 
		 * JVM detects it is running within an environment configured to support virtual threads, 
		 * the root ThreadGroup system maps certain unassigned or framework-spawned threads into a structural group called 'VirtualThreads' 
		 * for monitoring and management purposes. Still thread is not Virtual because Spring's AnnotationAsyncExecutionInterceptor completely 
		 * aborted its autoconfiguration lookup. It stopped looking for the virtual thread executor (applicationTaskExecutor) because we have
		 * configured beans for ThreadPoolTaskExecutor and taskScheduler. Else Spring would have executed it on virtual thread and 
		 * we would have seen 'Is Virtual Thread? true' in the output.
		 */
		System.out.println("Executing asyncOperation on " + Thread.currentThread().toString() 
	            + " | Is Virtual Thread? " + Thread.currentThread().isVirtual());
	    try {
	        // Virtual threads block efficiently on Thread.sleep without pinning the OS thread
	        Thread.sleep(2000); 
	    } catch (InterruptedException ignored) {
	        return CompletableFuture.completedFuture("interrupted-" + input);
	    }
	    return CompletableFuture.completedFuture("async-result-" + input);
	}
	
	@Async("transcodingPoolTaskExecutor")
	public CompletableFuture<String> asyncCustomOperation(String input) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ignored) {
		}
		return CompletableFuture.completedFuture("custom-async-result-" + input);
	}
	
	@Async("virtualPoolTaskExecutor")
	public CompletableFuture<String> asyncVirtualOperation(String input) {
		System.out.println("Executing asyncVirtualOperation on " + Thread.currentThread().toString() 
	            + " | Is Virtual Thread? " + Thread.currentThread().isVirtual() + ".....................................");
	    try {
	        // Virtual threads block efficiently on Thread.sleep without pinning the OS thread
	        Thread.sleep(2000); 
	    } catch (InterruptedException ignored) {
	        return CompletableFuture.completedFuture("interrupted-" + input);
	    }
	    return CompletableFuture.completedFuture("virtual-async-result-" + input);
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


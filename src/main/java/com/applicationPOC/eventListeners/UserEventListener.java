package com.applicationPOC.eventListeners;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.applicationPOC.event.UserCreatedEvent;
import com.applicationPOC.model.UserDto;

@Component
public class UserEventListener {

	// This fires IMMEDIATELY after save() but BEFORE commit
	@EventListener
	public void onProductCreated(UserCreatedEvent event) {
		UserDto user = event.getUser();
		System.out.printf("User created (pre-commit): %s \n.......", user.getName());
	}

	// This fires ONLY AFTER successful database commit
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async // optional: off main thread
	public void afterUserCommit(UserCreatedEvent event) {
		UserDto user = event.getUser();
		System.out.printf("User committed: %s (ID: %s) - sending email/cache update...", 
				user.getName(), user.getId());
		// Simulate async work: email, cache invalidation, analytics, etc.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	// This fires ONLY if transaction rolls back
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void afterRollback(UserCreatedEvent event) {
    	System.out.printf("User creation rolled back:%s.....", event.getUser().getName());
    }
}

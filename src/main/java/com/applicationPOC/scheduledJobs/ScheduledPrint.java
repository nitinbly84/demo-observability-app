package com.applicationPOC.scheduledJobs;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// spring.task.scheduling.pool.size=2 in application.properties to allow concurrent execution of scheduled tasks
// else only one task will run at a time, and the next task will wait for the previous one to finish, 
// which can lead to delays if tasks take longer than their scheduled intervals.
@Component
public class ScheduledPrint {

	// This method will run every 5 seconds after an initial delay of 2 seconds
	// fixedDelay means the next execution will start 5 seconds after the previous execution finishes
	// Due to Thread.sleep(5000), the task takes 5 seconds to complete, so the next execution will start 5 seconds after that, 
	// effectively running every 10 seconds, as seen in the logs.
	@Scheduled(initialDelay = 2l, fixedDelay = 5l, timeUnit = TimeUnit.SECONDS)
	public void printMessage() {
		System.out.println("Scheduled task executed at: " + LocalDateTime.now());
		// Simulate a long-running task
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// This method will run every 5 seconds regardless of how long the previous execution takes
	// fixedRate means the next execution will start 5 seconds after the previous execution starts, regardless of when it finishes
	// Due to Thread.sleep(5000), the task takes 5 seconds to complete, but the next execution will start immediately after the previous one starts
	// as seen in the logs.
	// It will run every 5 seconds even if the previous execution is still running, 
	// which can lead to overlapping executions if the task takes longer than the fixed rate.
	@Scheduled(fixedRate = 5l, timeUnit = TimeUnit.SECONDS)
	public void printMessage2() {
		System.out.println("Scheduled task2 executed at: " + LocalDateTime.now());
		// Simulate a long-running task
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

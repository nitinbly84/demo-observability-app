package com.applicationPOC.scheduledJobs;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

// This component demonstrates dynamic scheduling of tasks using TaskScheduler.
// The task will start running periodically only after the application is fully started, as it listens to the ApplicationReadyEvent.
// Scheduled tasks defined with @Scheduled will start immediately when the application context is initialized, regardless of the application state, 
// which may not be desirable in some cases.
@Component
public class DynamicScheduler {

    @Autowired
    private TaskScheduler taskScheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void startScheduling() {
        taskScheduler.scheduleAtFixedRate(() -> {
            System.out.println("Periodic task running only after application started...");
        }, Duration.ofSeconds(5));
    }
}

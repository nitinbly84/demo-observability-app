package com.applicationPOC.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.applicationPOC.config.AsyncConfig;           // replace with your actual async @Configuration class
import com.applicationPOC.event.UserCreatedEvent;
import com.applicationPOC.model.UserDto;
import com.applicationPOC.repository.BasicUserRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(SpringExtension.class)
@Import({DemoServiceTest.TestConfig.class, AsyncConfig.class})  // AsyncConfig brings in the real executor beans
class DemoServiceTest {

    @TestConfiguration
    @EnableCaching
    @EnableAsync
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("demoCacheDev");
        }

        @Bean
        BasicUserRepository userRepository() {
            return Mockito.mock(BasicUserRepository.class);
        }

        @Bean
        ApplicationEventPublisher eventPublisher() {
            return Mockito.mock(ApplicationEventPublisher.class);
        }

        @Bean
        DemoService demoService(BasicUserRepository userRepository,
                                       ApplicationEventPublisher eventPublisher) {
            return new DemoService(userRepository, eventPublisher);
        }
        
        @Bean(name = "transcodingPoolTaskExecutor")
    	Executor transcodingPoolTaskExecutor() {
    		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    		executor.setCorePoolSize(2);
    		executor.setMaxPoolSize(2);
    		executor.setQueueCapacity(500);
    		executor.setThreadNamePrefix("Custom Pool-");
    		// You should not call executor.initialize(), because it's called by Spring through InitializingBean.afterPropertiesSet()
    		// executor.initialize();
    		return executor;
    	}
    	
    	@Bean
    	Executor virtualPoolTaskExecutor() {
    		return Executors.newVirtualThreadPerTaskExecutor();
    	}
    }

    @Autowired
    private DemoService demoService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private BasicUserRepository userRepository;

    @MockitoBean
    private ApplicationEventPublisher publisher;

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    void expensiveCall_returnsExpectedData_withinAllowedTime() {
        assertThat(demoService.expensiveCall("user-1")).isEqualTo("data-for-user-1");
    }

    @Test
    void expensiveCall_populatesCacheEntry_afterFirstCall() {
        demoService.expensiveCall("user-cache-check");

        Cache.ValueWrapper entry = cacheManager.getCache("demoCacheDev").get("user-cache-check");

        assertThat(entry).isNotNull();
        assertThat(entry.get()).isEqualTo("data-for-user-cache-check");
    }

    @Test
    @Timeout(value = 4, unit = TimeUnit.SECONDS)
    void expensiveCall_doesNotReExecuteMethodBody_onCacheHit() {
        Logger demoLogger = (Logger) LoggerFactory.getLogger(DemoService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        demoLogger.addAppender(appender);

        try {
            demoService.expensiveCall("user-log-check");
            demoService.expensiveCall("user-log-check");
        } finally {
            demoLogger.detachAppender(appender);
        }

        long executionCount = appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("Executing expensiveCall for user-log-check"))
                .count();

        assertThat(executionCount).isEqualTo(1);
    }

    @Test
    void asyncOperation_completesWithExpectedResult() throws Exception {
        assertThat(demoService.asyncOperation("test").get(5, TimeUnit.SECONDS))
                .isEqualTo("async-result-test");
    }

    @Test
    void asyncOperation_runsOnNonVirtualThread_fromSpringDefaultAsyncExecutor() throws Exception {
        AtomicReference<Thread> executorThread = new AtomicReference<>();

        demoService.asyncOperation("check")
                .whenComplete((r, e) -> executorThread.set(Thread.currentThread()))
                .get(5, TimeUnit.SECONDS);

        assertThat(executorThread.get().isVirtual()).isFalse();
        assertThat(executorThread.get().getName()).contains("async-exec");
    }

    @Test
    void asyncCustomOperation_completesWithExpectedResult() throws Exception {
        assertThat(demoService.asyncCustomOperation("input").get(5, TimeUnit.SECONDS))
                .isEqualTo("custom-async-result-input");
    }

    @Test
    void asyncCustomOperation_runsOnThreadFromTranscodingExecutor() throws Exception {
        AtomicReference<Thread> executorThread = new AtomicReference<>();

        demoService.asyncCustomOperation("check")
                .whenComplete((r, e) -> executorThread.set(Thread.currentThread()))
                .get(5, TimeUnit.SECONDS);

        assertThat(executorThread.get().isVirtual()).isFalse();
        // thread name prefix must match what transcodingPoolTaskExecutor configures
        assertThat(executorThread.get().getName()).contains("Custom Pool-");
    }

    @Test
    void asyncVirtualOperation_completesWithExpectedResult() throws Exception {
        assertThat(demoService.asyncVirtualOperation("vt").get(5, TimeUnit.SECONDS))
                .isEqualTo("virtual-async-result-vt");
    }

    @Test
    void asyncVirtualOperation_runsOnVirtualThread_fromVirtualExecutor() throws Exception {
        AtomicReference<Thread> executorThread = new AtomicReference<>();

        demoService.asyncVirtualOperation("check")
                .whenComplete((r, e) -> executorThread.set(Thread.currentThread()))
                .get(5, TimeUnit.SECONDS);

        assertThat(executorThread.get().isVirtual()).isTrue();
    }

    @Test
    void saveUser_persistsUserViaRepository_andPublishesCreatedEvent() {
        UserDto input = new UserDto();
        input.setName("Alice");
        when(userRepository.save(any(UserDto.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto saved = demoService.saveUser(input);

        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getId()).isPositive();
        verify(userRepository, times(1)).save(any(UserDto.class));
        verify(publisher, times(1)).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void getUserById_returnsUser_whenFoundInRepository() {
        UserDto user = new UserDto();
        user.setId(10L);
        user.setName("Bob");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        Optional<UserDto> result = demoService.getUserById(10L);

        assertThat(result).isPresent().contains(user);
        verify(userRepository, times(1)).findById(10L);
    }

    @Test
    void getUserById_returnsEmpty_whenNotFoundInRepository() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<UserDto> result = demoService.getUserById(99L);

        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findById(99L);
    }
}

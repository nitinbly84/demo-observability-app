package com.applicationPOC.service;

import com.applicationPOC.togglzFeature.Features;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@Import(FeatureServiceTest.TestConfig.class)
class FeatureServiceTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        InMemoryStateRepository stateRepository() {
            return new InMemoryStateRepository();
        }

        @Bean
        FeatureManager featureManager(InMemoryStateRepository stateRepository) {
            return new FeatureManagerBuilder()
                    .featureEnum(Features.class)
                    .stateRepository(stateRepository)
                    .userProvider(new NoOpUserProvider())
                    .build();
        }

        @Bean
        FeatureService featureService() {
            return new FeatureService();
        }
    }

    @Autowired
    private FeatureService featureService;

    @Autowired
    private InMemoryStateRepository stateRepository;

    @BeforeEach
    void disableAllFeatures() {
        for (Features feature : Features.values()) {
            stateRepository.setFeatureState(new FeatureState(feature, false));
        }
    }

    // ─── Feature 1 ───────────────────────────────────────────────────────────

    @Test
    void feature1_returnsAvailable_whenEnabled() {
        stateRepository.setFeatureState(new FeatureState(Features.IS_FEATURE1_ENABLED, true));

        assertThat(featureService.isFeature1Available()).isEqualTo("Feature 1 is available");
    }

    @Test
    void feature1_returnsNotAvailable_whenDisabled() {
        assertThat(featureService.isFeature1Available()).isEqualTo("Feature 1 is not available");
    }

    // ─── Feature 2 ───────────────────────────────────────────────────────────

    @Test
    void feature2_returnsAvailable_whenEnabled() {
        stateRepository.setFeatureState(new FeatureState(Features.IS_FEATURE2_ENABLED, true));

        assertThat(featureService.isFeature2Available()).isEqualTo("Feature 2 is available");
    }

    @Test
    void feature2_returnsNotAvailable_whenDisabled() {
        assertThat(featureService.isFeature2Available()).isEqualTo("Feature 2 is not available");
    }

    // ─── Feature 3 ───────────────────────────────────────────────────────────

    @Test
    void feature3_returnsAvailable_whenEnabled() {
        stateRepository.setFeatureState(new FeatureState(Features.IS_FEATURE3_ENABLED, true));

        assertThat(featureService.isFeature3Available()).isEqualTo("Feature 3 is available");
    }

    @Test
    void feature3_returnsNotAvailable_whenDisabled() {
        assertThat(featureService.isFeature3Available()).isEqualTo("Feature 3 is not available");
    }

    // ─── isolation ───────────────────────────────────────────────────────────

    @Test
    void enablingOneFeature_doesNotAffectOthers() {
        stateRepository.setFeatureState(new FeatureState(Features.IS_FEATURE1_ENABLED, true));

        assertThat(featureService.isFeature1Available()).isEqualTo("Feature 1 is available");
        assertThat(featureService.isFeature2Available()).isEqualTo("Feature 2 is not available");
        assertThat(featureService.isFeature3Available()).isEqualTo("Feature 3 is not available");
    }

    @Test
    void allFeaturesEnabled_allReturnAvailable() {
        for (Features feature : Features.values()) {
            stateRepository.setFeatureState(new FeatureState(feature, true));
        }

        assertThat(featureService.isFeature1Available()).isEqualTo("Feature 1 is available");
        assertThat(featureService.isFeature2Available()).isEqualTo("Feature 2 is available");
        assertThat(featureService.isFeature3Available()).isEqualTo("Feature 3 is available");
    }
}

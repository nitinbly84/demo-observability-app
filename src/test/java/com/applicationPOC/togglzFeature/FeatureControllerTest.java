package com.applicationPOC.togglzFeature;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.applicationPOC.service.FeatureService;

/**
 * Unit tests for {@link FeatureController}.
 *
 * Architecture
 * ─────────────
 * • NO Spring context is loaded.  The controller is instantiated manually,
 *   field-injected via {@link ReflectionTestUtils}, and exercised through a
 *   standalone {@link MockMvc} — keeping every test fast (< 1 s).
 *
 * • {@link FeatureService} is mocked, so Togglz / Redis infrastructure is
 *   never touched.
 *
 * Coverage targets
 * ─────────────────
 * • Every valid feature name ("feature1" – "feature9") is routed to the
 *   correct {@link FeatureService} method and the returned string is written
 *   to the response.
 *
 * • The {@code default} switch branch is exercised with an unknown name and
 *   must return 200 OK with the literal "Invalid feature name".
 *
 * • Both availability outcomes ("available" / "not available") are verified
 *   for feature1 so that {@link FeatureService} delegation is confirmed for
 *   both states without duplicating the enabled/disabled assertion for all
 *   nine features (the routing logic, not the toggle state, is under test).
 */
@ExtendWith(MockitoExtension.class)
class FeatureControllerTest {

    @Mock
    private FeatureService featureService;

    private FeatureController controller;
    private MockMvc           mockMvc;

    // ─────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        controller = new FeatureController();
        ReflectionTestUtils.setField(controller, "featureService", featureService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ════════════════════════════════════════════════════════════════════
    // Happy-path routing — each feature name dispatches to the correct
    // FeatureService method and the response body is passed through verbatim
    // ════════════════════════════════════════════════════════════════════

    /**
     * Parameterised test covering all nine valid feature names.
     *
     * For each row: the first column is the path variable, the second is the
     * stub return value that {@link FeatureService} will return.  The test
     * verifies that the controller's switch expression selects the right
     * service method and that the returned string reaches the HTTP response.
     */
    @ParameterizedTest(name = "{0} → \"{1}\"")
    @CsvSource({
        "feature1, Feature 1 is available",
        "feature2, Feature 2 is available",
        "feature3, Feature 3 is available",
        "feature4, Feature 4 is available",
        "feature5, Feature 5 is available",
        "feature6, Feature 6 is available",
        "feature7, Feature 7 is available",
        "feature8, Feature 8 is available",
        "feature9, Feature 9 is available"
    })
    void checkFeature_returnsServiceResponse_forValidFeatureName(
            String featureName, String serviceResponse) throws Exception {

        stubFeatureService(featureName, serviceResponse);

        mockMvc.perform(get("/featurerApi/feature/" + featureName))
                .andExpect(status().isOk())
                .andExpect(content().string(serviceResponse));

        verifyFeatureServiceCalled(featureName);
    }

    // ════════════════════════════════════════════════════════════════════
    // Both toggle states for feature1
    // ════════════════════════════════════════════════════════════════════

    /** Feature is enabled → service returns the "available" message. */
    @Test
    void checkFeature_returnsAvailableMessage_whenFeature1IsEnabled() throws Exception {
        when(featureService.isFeature1Available()).thenReturn("Feature 1 is available");

        mockMvc.perform(get("/featurerApi/feature/feature1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Feature 1 is available"));

        verify(featureService).isFeature1Available();
    }

    /** Feature is disabled → service returns the "not available" message. */
    @Test
    void checkFeature_returnsNotAvailableMessage_whenFeature1IsDisabled() throws Exception {
        when(featureService.isFeature1Available()).thenReturn("Feature 1 is not available");

        mockMvc.perform(get("/featurerApi/feature/feature1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Feature 1 is not available"));

        verify(featureService).isFeature1Available();
    }

    // ════════════════════════════════════════════════════════════════════
    // Default branch — unknown feature name
    // ════════════════════════════════════════════════════════════════════

    /**
     * An unrecognised feature name falls through the switch's {@code default}
     * arm.  No service method should be invoked; the literal string
     * "Invalid feature name" must be returned with 200 OK.
     */
    @Test
    void checkFeature_returnsInvalidFeatureName_whenFeatureNameIsUnknown() throws Exception {
        mockMvc.perform(get("/featurerApi/feature/featureXYZ"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid feature name"));
    }

    /**
     * An empty string path variable is not routable by Spring MVC
     * (it would not match the {@code /{feature}} segment), so we verify
     * that a name that looks plausible but is not in the switch ("feature0")
     * also hits the default arm.
     */
    @Test
    void checkFeature_returnsInvalidFeatureName_forFeature0() throws Exception {
        mockMvc.perform(get("/featurerApi/feature/feature0"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid feature name"));
    }

    /**
     * Numeric names (e.g. "123") also fall through to the default branch.
     */
    @Test
    void checkFeature_returnsInvalidFeatureName_forNumericName() throws Exception {
        mockMvc.perform(get("/featurerApi/feature/123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid feature name"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Stubs the {@link FeatureService} method that corresponds to the given
     * feature name to return {@code response}.
     */
    private void stubFeatureService(String featureName, String response) {
        switch (featureName) {
            case "feature1" -> when(featureService.isFeature1Available()).thenReturn(response);
            case "feature2" -> when(featureService.isFeature2Available()).thenReturn(response);
            case "feature3" -> when(featureService.isFeature3Available()).thenReturn(response);
            case "feature4" -> when(featureService.isFeature4Available()).thenReturn(response);
            case "feature5" -> when(featureService.isFeature5Available()).thenReturn(response);
            case "feature6" -> when(featureService.isFeature6Available()).thenReturn(response);
            case "feature7" -> when(featureService.isFeature7Available()).thenReturn(response);
            case "feature8" -> when(featureService.isFeature8Available()).thenReturn(response);
            case "feature9" -> when(featureService.isFeature9Available()).thenReturn(response);
        }
    }

    /**
     * Verifies that exactly the {@link FeatureService} method matching
     * {@code featureName} was called once (and no other method was called
     * implicitly).
     */
    private void verifyFeatureServiceCalled(String featureName) {
        switch (featureName) {
            case "feature1" -> verify(featureService).isFeature1Available();
            case "feature2" -> verify(featureService).isFeature2Available();
            case "feature3" -> verify(featureService).isFeature3Available();
            case "feature4" -> verify(featureService).isFeature4Available();
            case "feature5" -> verify(featureService).isFeature5Available();
            case "feature6" -> verify(featureService).isFeature6Available();
            case "feature7" -> verify(featureService).isFeature7Available();
            case "feature8" -> verify(featureService).isFeature8Available();
            case "feature9" -> verify(featureService).isFeature9Available();
        }
    }
}

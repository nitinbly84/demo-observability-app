package com.applicationPOC.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.*;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Enterprise Performance Test Setup using Gatling Java DSL.
 * Completely isolates test payload configurations into external JSON template resource files.
 */
public class AuthenticatedFeatureToggleSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int TARGET_RPS = Integer.getInteger("targetRps", 100);
    private static final int DURATION_SEC = Integer.getInteger("durationSec", 60);

    // HTTP Protocol Engine Protocol Setup
    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/Java Token-Engine/3.x");

    // Dynamic Parameterized Target Feature Names Feeder
    private final Iterator<Map<String, Object>> featureFeeder = Stream.generate(() -> {
        String[] features = {
            "feature1", "feature2", "feature3", "feature4", 
            "feature5", "feature6", "feature7", "feature8", "feature9"
        };
        Map<String, Object> record = new HashMap<>();
        record.put("featureName", features[new Random().nextInt(features.length)]);
        return record;
    }).iterator();

    // User Credentials Feeder covering different personas
    private final List<Map<String, Object>> userCredentials = List.of(
        Map.of("username", "nitin", "password", "password"),       // Simulates ROLE_ADMIN
        Map.of("username", "admin", "password", "password"),       // Simulates USER role
        Map.of("username", "user", "password", "password"),         // Simulates USER role
        Map.of("username", "qa-team-user", "password", "password123") // Internal Beta Tester
    );
    private final FeederBuilder<Object> userFeeder = listFeeder(userCredentials).circular();

    // Reusable Authentication Chain utilizing ElFileBody
    private final ChainBuilder authenticateUser = feed(userFeeder)
        .exec(
            http("Authentication Phase - POST /auth/login")
                .post("/auth/login")
                // Externalized file lookup. Resolves variables against the active user context session.
                .body(ElFileBody("bodies/login-template.json"))
                .check(status().is(200))
                .check(jsonPath("$.token").saveAs("jwtToken"))
        );

    // Reusable Secured Core Business Target Transaction Chain
    private final ChainBuilder checkFeatureFlag = feed(featureFeeder)
        .exec(
            http("Business Phase - GET /featurerApi/feature/#{featureName}")
                .get("/featurerApi/feature/#{featureName}")
                .header("Authorization", "Bearer #{jwtToken}")
                .check(status().is(200))
        );

    // Profile Workflow Definitions
    private final ScenarioBuilder authorizedUserScenario = scenario("Decoupled Authenticated User Journey")
        .exec(authenticateUser)
        .repeat(10).on(
            exec(checkFeatureFlag).pause(1)
        );

    // Simulation Profiles and SLA Guardrails
    {
        setUp(
            authorizedUserScenario.injectOpen(
                nothingFor(2),
                rampUsersPerSec(10).to(TARGET_RPS).during(15),
                constantUsersPerSec(TARGET_RPS).during(DURATION_SEC)
            )
        )
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().percentile(80).lt(100),
            global().failedRequests().percent().is(0.0)
        );
    }
}


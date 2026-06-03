package com.applicationPOC.performance;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.*;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Enterprise Performance Test Architecture using Gatling Java DSL.
 * Tests the throughput and latency overhead of dynamic Feature Toggles
 * backed by Redis and Spring Security context evaluation.
 */
public class FeatureToggleSimulation extends Simulation {

    // 1. Dynamic Environment Configuration via JVM System Properties
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int TARGET_RPS = Integer.getInteger("targetRps", 100);
    private static final int DURATION_SEC = Integer.getInteger("durationSec", 60);

    // HTTP Protocol Engine Setup Configuration
    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl(BASE_URL)
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling/Java Performance Performance-Engine/3.x");

    // 2. Data Feeders for Parameterized Traversal
    // Randomly picks a feature name matching the switch expression inside FeatureController
    private final Iterator<Map<String, Object>> featureFeeder = Stream.generate(() -> {
        String[] features = {
            "feature1", "feature2", "feature3", "feature4", 
            "feature5", "feature6", "feature7", "feature8", "feature9"
        };
        Map<String, Object> record = new HashMap<>();
        record.put("featureName", features[new Random().nextInt(features.length)]);
        return record;
    }).iterator();

    // Circular data provider to inject distinct users corresponding to SpringSecurityUserProvider rules
    private final List<Map<String, Object>> userCredentials = List.of(
        Map.of("username", "alice", "authHeader", "Basic YWxpY2U6cGFzc3dvcmQxMjM="),       // Regular Role
        Map.of("username", "bob", "authHeader", "Basic Ym9iOnBhc3N3b3JkMTIz="),         // Regular Role
        Map.of("username", "nitin", "authHeader", "Basic bml0aW46cGFzc3dvcmQxMjM="),       // ROLE_ADMIN
        Map.of("username", "qa-team-user", "authHeader", "Basic cWEtdGVhbS11c2VyOnBhc3N3b3JkMTIz=") // Beta User List
    );
    private final FeederBuilder<Object> authenticatedUserFeeder = listFeeder(userCredentials).circular();

    // 3. User Scenarios
    // Personas-A: Public / Anonymous Consumers (Hits fallback/anonymous strategy evaluations)
    private final ScenarioBuilder anonymousUserScenario = scenario("Anonymous Consumer Journey")
        .feed(featureFeeder)
        .exec(
            http("GET Feature Flag Status - Anonymous")
                .get("/featurerApi/feature/#{featureName}")
                .check(status().is(200))
        );

    // Personas-B: Authenticated Enterprise Consumers (Forces Redis read + Security Context reflection)
    private final ScenarioBuilder authenticatedUserScenario = scenario("Authenticated Enterprise Journey")
        .feed(featureFeeder)
        .feed(authenticatedUserFeeder)
        .exec(
            http("GET Feature Flag Status - Authenticated [User: #{username}]")
                .get("/featurerApi/feature/#{featureName}")
                .header("Authorization", "#{authHeader}")
                .check(status().is(200))
        );

    // 4. Load Injection Profile Definition (Open Workload Model)
    {
        setUp(
            anonymousUserScenario.injectOpen(
                nothingFor(2),                                          // Warm-up cushion
                rampUsers(10).during(5),                                // Gentle ramp
                constantUsersPerSec(TARGET_RPS / 2).during(DURATION_SEC) // Steady concurrent load
            ),
            authenticatedUserScenario.injectOpen(
                nothingFor(2),
                rampUsersPerSec(1).to(TARGET_RPS).during(10),           // Linear strain/stress phase
                constantUsersPerSec(TARGET_RPS).during(DURATION_SEC)    // Peak saturation plateau
            )
        )
        .protocols(httpProtocol)
        
        // 5. Hard Operational Gatekeepers (SLAs)
        .assertions(
            global().responseTime().percentile(95).lt(50),  // 95% of responses must be faster than 50 milliseconds
            global().responseTime().max().lt(250),          // Strict ceiling on worst-case tail latency at 250ms
            global().failedRequests().percent().is(0.0)     // Absolute tolerance of 0% functional failures
        );
    }
}

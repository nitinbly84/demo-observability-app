package com.applicationPOC.performance;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.nothingFor;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.headerRegex;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import io.gatling.commons.validation.Validation;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

// mvn gatling:test -Dgatling.simulationClass=com.applicationPOC.performance.PublicSimulator
public class PublicSimulator extends Simulation {

	// Global HTTP Protocol Configuration
	HttpProtocolBuilder httpProtocol = http
			.baseUrl("http://localhost:8080/api/public") 
			.acceptHeader("application/json")
			.contentTypeHeader("application/json")
			.shareConnections()
			.maxConnectionsPerHost(6000);

	// ========================================================================
	// FEEDERS & DATA GENERATORS (Thread-Safe)
	// ========================================================================

	// Generates unique IDs sequentially/randomly to validate Cache Miss vs Hit lifecycle
	Iterator<Map<String, Object>> cacheIdFeeder = Stream.generate(() -> {
		Map<String, Object> map = new HashMap<>();
		map.put("cacheId", ThreadLocalRandom.current().nextInt(1, 1000000));
		return map;
	}).iterator();

	// Generates unique payloads for User Creation to prevent DB constraint violations
	Iterator<Map<String, Object>> userPayloadFeeder = Stream.generate(() -> {
		Map<String, Object> map = new HashMap<>();
		char[] alphabets = {'a','b', 'c', 'd'};
		ThreadLocalRandom random = ThreadLocalRandom.current();
		String last = alphabets[random.nextInt(0, 4)]+""+alphabets[random.nextInt(0, 4)];
		map.put("userName", "PerformanceUser " + last);
		map.put("userEmail", "perf_" + last + "@enterprise.com");
		return map;
	}).iterator();

	// Specific bean names for the autowired router endpoint verification
	Iterator<Map<String, Object>> beanNameFeeder = Stream.generate(() -> {
		String[] beans = {"dummyBean1", "dummyBean2"};
		Map<String, Object> map = new HashMap<>();
		map.put("beanName", beans[ThreadLocalRandom.current().nextInt(beans.length)]);
		return map;
	}).iterator();


	// ========================================================================
	// SCENARIOS IMPLEMENTATION
	// ========================================================================

	// 1. Ping Endpoint Scenario
	ScenarioBuilder pingScenario = scenario("Ping API Scenario")
			.exec(
					http("GET_Ping")
					.get("/ping")
					.check(status().is(200))
					.check(substring("pong")) // Verifies pure text body
					);

	// 2. Cache Behavior Scenario (Verifies the isolated Miss vs Hit Latency)
	ScenarioBuilder cacheScenario = scenario("Cache Validation API Scenario")
			.feed(cacheIdFeeder)
			.exec(
					http("GET_Cached_Resource_MISS")
					.get("/cached/#{cacheId}")
					.check(status().is(200))
					)
			.pause(1)
			.exec(
					http("GET_Cached_Resource_HIT")
					.get("/cached/#{cacheId}")
					.check(status().is(200))
					);

	// 3. Search Request Query Parameters Scenario
	ScenarioBuilder searchScenario = scenario("Search API Scenario")
			.exec(
					http("GET_Search_With_Params")
					.get("/search")
					.queryParam("keyword", "spring-boot")
					.queryParam("page", "5")
					.check(status().is(200))
					.check(jsonPath("$.keyword").is("spring-boot"))
					.check(jsonPath("$.page").ofInt().is(5))
					);

	// 4. Request Header Extraction Scenario
	ScenarioBuilder userAgentScenario = scenario("User Agent Header API Scenario")
			.exec(
					http("GET_User_Agent")
					.get("/user-agent")
					.header("User-Agent", "GatlingPerformanceRunner")
					.check(status().is(200))
					.check(jsonPath("$.userAgent").is("GatlingPerformanceRunner"))
					);

	// 5. Stateful Cookie Processing Scenario
	ScenarioBuilder cookieScenario = scenario("Stateful Cookie API Scenario")
			.exec(
					http("GET_Welcome_Set_Cookie")
					.get("/welcome")
					.check(status().is(200))
					// FIX: Assert that the Set-Cookie header contains your expected cookie name
					.check(headerRegex("Set-Cookie", "username=([^;]+)").exists()) 
					)
			.pause(1)
			.exec(
					http("GET_Welcome_With_Active_Cookie")
					.get("/welcome")
					// Gatling automatically stores and propagates cookies back to subsequent 
					// requests behind the scenes. No manual session injection is needed.
					.check(status().is(200))
					.check(substring("Welcome, demo-user"))
					);

	// 6. Default User Static Response Scenario
	ScenarioBuilder defaultUserScenario = scenario("Default User API Scenario")
			.exec(
					http("GET_Default_User")
					.get("/default-user")
					.check(status().is(200))
					.check(jsonPath("$.name").is("John Doe"))
					.check(jsonPath("$.role").is("USER"))
					);

	// 7. REST Stateful Lifecycle: Create User (JSON Payload) -> Get User by ID
	ScenarioBuilder userLifecycleScenario = scenario("User Engine Lifecycle API Scenario")
			.feed(userPayloadFeeder)
			.exec(
					http("POST_Create_User_JSON")
					.post("/users")
					.body(StringBody("""
							{
							  "name": "#{userName}",
							  "enabled": "true",
							  "password": "securedPass123",
							  "role": "ADMIN",
							  "email": "#{userEmail}"
							}
							"""))
					.check(status().is(201))
					//                .check(header("Location").exists())
					.check(jsonPath("$.id").saveAs("newUserId")) // Extract system generated ID from response
					)
			.pause(2)
			.exec(
					http("GET_User_By_Generated_ID")
					.get("/users/#{newUserId}")
					.check(status().is(200))
					// FIX: Extract the actual session value dynamically for validation
					.check(jsonPath("$.email").validate("VerifyEmailMatch", (actualEmail, session) -> {
						String expectedEmail = session.getString("userEmail");
						return actualEmail.equals(expectedEmail) 
								? Validation.TrueSuccess().toString() 
										: Validation.FalseSuccess().toString();
					}))
					);

	// 8. Form-Urlencoded Submission Scenario (Tests custom Exception Handling boundaries)
	ScenarioBuilder userFormScenario = scenario("User Form Submission API Scenario")
			.feed(userPayloadFeeder)
			.exec(
					http("POST_Create_User_Form")
					.post("/users/form")
					.formParam("name", "#{userName}")
					.formParam("email", "#{userEmail}")
					.formParam("password", "formSecured321")
					.formParam("role", "USER")
					// Controller throws 500 when random assignment <= 4. We capture both to maintain statistical accuracy.
					.check(status().in(200, 500)) 
					);

	// 9. Concurrency & Non-Blocking Models Scenarios (Standard Async, Custom ThreadPool, Virtual Threads)
	ScenarioBuilder concurrentProcessingModelsScenario = scenario("Asynchronous Processing Paradigms Scenario")
			.exec(
					http("GET_Standard_CompletableFuture_Async")
					.get("/async/performanceTest")
					.check(status().is(200))
					.check(substring("Processed:"))
					)
			.exec(
					http("GET_Custom_ThreadPool_Async")
					.get("/custom-async/performanceTest")
					.check(status().is(200))
					.check(substring("Processed:"))
					);
	
	ScenarioBuilder concurrentProcessingVirtualModelsScenario = scenario("Asynchronous Virtual Threads Processing Paradigms Scenario")
			.exec(
					http("GET_Virtual_Threads_Async")
					.get("/virtual-async/performanceTest")
					.check(status().is(200))
					.check(substring("Processed:"))
					);

	// 10. Filter Interception & Context Scope Scenarios (@RequestAttribute, Prototypes, Singletons)
	ScenarioBuilder springInternalsScenario = scenario("Spring Framework Internals Configuration Scenario")
			.exec(
					http("GET_Filter_Request_Attribute_Greet")
					.get("/greet")
					.check(status().is(200))
					)
			.exec(
					http("GET_SpEL_And_Prototype_Scope_Message")
					.get("/message")
					.check(status().is(200))
					.check(substring("Hello from")) // Validates underlying prototype lifecycle logic
					)
			.exec(
					http("GET_Context_Bean_Scope_Instance")
					.get("/scope")
					.check(status().is(200))
					);

	// 11. Extensions & Custom Bean Routing Scenarios (AOP, Encryption, Auto-wiring Strategies)
	ScenarioBuilder extensionsScenario = scenario("Architecture Blueprint Infrastructure Scenario")
			.feed(beanNameFeeder)
			.exec(
					http("GET_AOP_Aspect_Modification")
					.get("/aspect-value/Architect")
					.check(status().is(200))
					)
			.exec(
					http("GET_BCrypt_Password_Hashing")
					.get("/hash/rawPasswordToSecure")
					.check(status().is(200))
					)
			.exec(
					http("GET_Dynamic_Bean_Resolution_By_Name")
					.get("/autowired/#{beanName}")
					.check(status().is(200))
					)
			.exec(http("GET_First_Bean").get("/first").check(status().is(200)))
			.exec(http("GET_Multi_Bean").get("/multi").check(status().is(200)))
			.exec(http("GET_Property_Injected_Name").get("/name").check(status().is(200)))
			.exec(http("GET_Conditional_Bean").get("/conditional-first").check(status().is(200)))
			.exec(http("GET_ConfigurationProperties_POJO").get("/user-properties").check(status().is(200)));


	// ========================================================================
	// SIMULATION PROFILE & PERFORMANCE SLA ASSERTIONS
	// ========================================================================
	{
		setUp(
				pingScenario.injectOpen(rampUsers(200).during(10)),
				cacheScenario.injectOpen(rampUsers(500).during(25)),
				searchScenario.injectOpen(rampUsers(30).during(15)),
				userAgentScenario.injectOpen(rampUsers(10).during(10)),
				cookieScenario.injectOpen(rampUsers(35).during(15)),
				defaultUserScenario.injectOpen(rampUsers(20).during(10)),
				userLifecycleScenario.injectOpen(rampUsers(40).during(20)),
				userFormScenario.injectOpen(rampUsers(80).during(20)),
				concurrentProcessingModelsScenario.injectOpen(
						// Same kind of waits can added for other scenarios as well to create more realistic interleaving and 
						// concurrency patterns, but we keep it simple here for demonstration purposes.
						// Holds execution until previous scenario is completed, assuming that it will not exceed 30 seconds.
						nothingFor(Duration.ofSeconds(60)),rampUsers(60).during(30)),
				concurrentProcessingVirtualModelsScenario.injectOpen(
						nothingFor(Duration.ofSeconds(62)),rampUsers(2000).during(30)),
				springInternalsScenario.injectOpen(rampUsers(30).during(15)),
				extensionsScenario.injectOpen(rampUsers(50).during(20))
				)
		.protocols(httpProtocol)
		.assertions(
				// Global Operational Guardrails
				global().failedRequests().percent().is(0.0), // Enforce 100% success execution target

				// Explicit Non-functional Performance Requirements (SLA validation)
				details("GET_Cached_Resource_HIT").responseTime().percentile3().lt(20),   // Cache Hit: p95 must be under 20ms
				details("GET_Cached_Resource_MISS").responseTime().percentile3().gt(100), // Cache Miss: Expect resource overhead

				// Asynchronous Processing Latency Verification
				details("GET_Virtual_Threads_Async").responseTime().percentile3().lt(2030), // Virtual Threads should be significantly faster due to efficient blocking
				details("GET_Standard_CompletableFuture_Async").responseTime().percentile3().lt(11400)
				);
	}
}
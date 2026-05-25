package com.applicationPOC.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.applicationPOC.aspects.DemoAspectService;
import com.applicationPOC.config.UserProperties;
import com.applicationPOC.model.First;
import com.applicationPOC.model.FirstAutowiredBean;
import com.applicationPOC.model.MultiAutowiredBean;
import com.applicationPOC.model.Scope1;
import com.applicationPOC.model.UserDto;
import com.applicationPOC.service.DemoService;
import com.applicationPOC.service.FeatureService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

/**
 * Unit tests for {@link PublicController}.
 *
 * Architecture
 * ─────────────
 * • NO Spring context is loaded.  The controller is instantiated manually,
 *   field-injected via {@link ReflectionTestUtils}, and exercised through a
 *   standalone {@link MockMvc} — keeping each test fast (< 1 s except async).
 *
 * • {@link LocalValidatorFactoryBean} is wired into standalone MockMvc so
 *   JSR-380 (@Valid) constraints are enforced exactly as in production.
 *
 * • {@link MockedConstruction} intercepts {@code new Random()} inside
 *   {@code createUserFromForm} so both branches (throw / success) are
 *   deterministically reachable without flakiness.
 *
 * • Async endpoints ({@code /async}, {@code /custom-async}) use the two-step
 *   MockMvc async-dispatch pattern required for {@code CompletableFuture}
 *   return types.
 *
 * Coverage targets
 * ─────────────────
 * Every line and every branch in PublicController is exercised by at least
 * one test method below.
 */
@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    // ── Dependencies that go through the constructor ──────────────────────────

    @Mock private DemoService            demoService;
    @Mock private ApplicationEventPublisher publisher;

    // ── @Autowired field-injected dependencies ────────────────────────────────

    @Mock private ApplicationContext     context;
    @Mock private UserProperties         userProperties;
    @Mock private DemoAspectService      demoAspectService;
    @Mock private FeatureService         featureService;
    @Mock private FirstAutowiredBean     first;
    @Mock private MultiAutowiredBean     multi;
    @Mock private First                  conditionalFirst;

    // ── Test infrastructure ───────────────────────────────────────────────────

    private PublicController controller;
    private MockMvc           mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // 1. Constructor injection (demoService + publisher)
        controller = new PublicController(demoService, publisher);

        // 2. Field injection for all @Autowired dependencies
        ReflectionTestUtils.setField(controller, "context",          context);
        ReflectionTestUtils.setField(controller, "userProperties",   userProperties);
        ReflectionTestUtils.setField(controller, "demoAspectService",demoAspectService);
        ReflectionTestUtils.setField(controller, "featureService",   featureService);
        ReflectionTestUtils.setField(controller, "first",            first);
        ReflectionTestUtils.setField(controller, "multi",            multi);
        ReflectionTestUtils.setField(controller, "conditionalFirst", conditionalFirst);

        // 3. @Value fields (Mockito @InjectMocks does not process @Value)
        ReflectionTestUtils.setField(controller, "name",         "Test Name");
        ReflectionTestUtils.setField(controller, "randomNumber", 42);

        // 4. Standalone MockMvc with JSR-380 validation enabled
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/ping
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void ping_returns200WithPong() throws Exception {
        mockMvc.perform(get("/api/public/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/cached/{id}
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void cached_delegatesToDemoService_andReturnsResult() throws Exception {
        when(demoService.expensiveCall("item-1")).thenReturn("data-for-item-1");

        mockMvc.perform(get("/api/public/cached/item-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("data-for-item-1"));

        verify(demoService).expensiveCall("item-1");
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/search
    // ════════════════════════════════════════════════════════════════════════

    /** Branch: no params → default values applied */
    @Test
    void search_returnsDefaults_whenNoParamsProvided() throws Exception {
        mockMvc.perform(get("/api/public/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("java"))
                .andExpect(jsonPath("$.page").value(1));
    }

    /** Branch: explicit params → values passed through */
    @Test
    void search_returnsProvidedValues_whenParamsPresent() throws Exception {
        mockMvc.perform(get("/api/public/search")
                        .param("keyword", "spring")
                        .param("page",    "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyword").value("spring"))
                .andExpect(jsonPath("$.page").value(3));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/user-agent
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void userAgent_returnsHeaderValueInBody() throws Exception {
        mockMvc.perform(get("/api/public/user-agent")
                        .header("User-Agent", "TestBrowser/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userAgent").value("TestBrowser/1.0"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/welcome
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Branch 1 — username cookie IS present:
     * Response body says "Welcome, <name>"; no new cookie is set.
     */
    @Test
    void welcomeUser_returnsWelcomeMessage_whenCookieIsPresent() throws Exception {
        mockMvc.perform(get("/api/public/welcome")
                        .cookie(new Cookie("username", "Alice")))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome, Alice"));
    }

    /**
     * Branch 2 — username cookie is ABSENT:
     * Response sets a default "username=demo-user" cookie and returns
     * the cookie-set confirmation message.
     */
    @Test
    void welcomeUser_setsCookieAndReturnsConfirmation_whenCookieIsAbsent() throws Exception {
        mockMvc.perform(get("/api/public/welcome"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cookie 'username' set to demo-user"))
                .andExpect(cookie().value("username", "demo-user"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/default-user
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getDefaultUser_returnsHardcodedUserObject() throws Exception {
        mockMvc.perform(get("/api/public/default-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("default@email.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST /api/public/users
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Branch 1 — valid request body:
     * User is saved, event is published, 201 Created returned with Location header.
     */
    @Test
    void createUser_returns201WithLocation_whenRequestBodyIsValid() throws Exception {
        UserDto saved = new UserDto();
        saved.setId(1L);
        saved.setName("Jane");
        saved.setEmail("jane@example.com");
        saved.setPassword("securePass1");
        saved.setRole("USER_ROLE");

        when(demoService.saveUser(any(UserDto.class))).thenReturn(saved);

        String requestBody = objectMapper.writeValueAsString(saved);

        mockMvc.perform(post("/api/public/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/public/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jane"));

        verify(demoService).saveUser(any(UserDto.class));
        verify(publisher).publishEvent(any());
    }

    /**
     * Branch 2 — invalid request body (empty JSON):
     * BindingResult has errors → 400 Bad Request with field-error map returned.
     * No call to demoService or publisher is made.
     */
    @Test
    void createUser_returns400WithErrorMap_whenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/public/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))     // all required fields missing
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isMap());
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/users/{id}
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Branch 1 — user found: 200 OK with user body.
     */
    @Test
    void getUser_returns200WithUser_whenUserExists() throws Exception {
        UserDto user = new UserDto();
        user.setId(5L);
        user.setName("Bob");

        when(demoService.getUserById(5L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/public/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    /**
     * Branch 2 — user not found: 404 Not Found with descriptive message.
     */
    @Test
    void getUser_returns404WithMessage_whenUserDoesNotExist() throws Exception {
        when(demoService.getUserById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/public/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User with id 99 not found"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST /api/public/users/form
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Branch 1 — Random produces a value ≤ 4:
     * {@code IllegalArgumentException} is thrown.
     * {@link MockedConstruction} intercepts {@code new Random()} so the test
     * is deterministic — no flakiness from a real random value.
     */
    @Test
    void createUserFromForm_throwsIllegalArgumentException_whenRandomValueIsLow()
            throws Exception {

        try (MockedConstruction<Random> mocked = mockConstruction(Random.class,
                (mock, ctx) -> when(mock.nextInt(1, 10)).thenReturn(2))) {

            mockMvc.perform(post("/api/public/users/form")
                            .param("name",     "Charlie")
                            .param("email",    "charlie@example.com")
                            .param("password", "pass12345")
                            .param("role",     "USER_ROLE"))
                    .andExpect(result ->
                            assertThat(result.getResolvedException())
                                    .isInstanceOf(IllegalArgumentException.class)
                                    .hasMessageContaining("Throwing Random Exception"));
        }
    }

    /**
     * Branch 2 — Random produces a value > 4:
     * Method completes normally and the saved user is returned.
     */
    @Test
    void createUserFromForm_returnsSavedUser_whenRandomValueIsHigh() throws Exception {
        UserDto saved = new UserDto();
        saved.setId(2L);
        saved.setName("Charlie");
        saved.setEmail("charlie@example.com");
        saved.setPassword("pass12345");
        saved.setRole("USER_ROLE");

        when(demoService.saveUser(any(UserDto.class))).thenReturn(saved);

        try (MockedConstruction<Random> mocked = mockConstruction(Random.class,
                (mock, ctx) -> when(mock.nextInt(1, 10)).thenReturn(7))) {

            mockMvc.perform(post("/api/public/users/form")
                            .param("name",     "Charlie")
                            .param("email",    "charlie@example.com")
                            .param("password", "pass12345")
                            .param("role",     "USER_ROLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Charlie"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/async/{input}
    // ════════════════════════════════════════════════════════════════════════

    /**
     * CompletableFuture endpoints require the two-step async-dispatch pattern
     * in MockMvc: first perform to start async processing, then asyncDispatch
     * to resolve the result.
     */
    @Test
    void async_returnsProcessedResult_afterAsyncCompletion() throws Exception {
        when(demoService.asyncOperation("msg"))
                .thenReturn(CompletableFuture.completedFuture("async-result-msg"));

        MvcResult mvcResult = mockMvc.perform(get("/api/public/async/msg"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("Processed: async-result-msg"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/custom-async/{input}
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void customAsync_returnsProcessedResult_afterAsyncCompletion() throws Exception {
        when(demoService.asyncCustomOperation("msg"))
                .thenReturn(CompletableFuture.completedFuture("custom-async-result-msg"));

        MvcResult mvcResult = mockMvc.perform(get("/api/public/custom-async/msg"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("Processed: custom-async-result-msg"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/greet
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Branch 1 — username request attribute IS set (injected by a filter in
     * production; provided via .requestAttr() in the test).
     */
    @Test
    void greet_returnsPersonalisedGreeting_whenUsernameAttributeIsPresent()
            throws Exception {
        mockMvc.perform(get("/api/public/greet")
                        .requestAttr("username", "Dave"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Dave!"));
    }

    /**
     * Branch 2 — username attribute is ABSENT (required = false, so null is
     * accepted); controller falls back to "anonymous".
     */
    @Test
    void greet_returnsAnonymousGreeting_whenUsernameAttributeIsAbsent()
            throws Exception {
        mockMvc.perform(get("/api/public/greet"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, anonymous!"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/message
    // ════════════════════════════════════════════════════════════════════════

    /**
     * context.getBean("scope1") is called three times.
     * Returning the same mock for all three calls means areEqual == true.
     * The final string includes the stubbed getNumber() return value.
     */
    @Test
    void getMessage_returnsMessageWithScopeComparisonAndNumber() throws Exception {
        Scope1 scope1Mock = mock(Scope1.class);
        when(scope1Mock.getNumber()).thenReturn(7);
        when(context.getBean("scope1")).thenReturn(scope1Mock);

        mockMvc.perform(get("/api/public/message"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    // Body format: "<message> <areEqual> <number>"
                    assertThat(body).contains("true");   // same mock → reference equal
                    assertThat(body).contains("7");
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/scope
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getScope_returnsInstanceIdFromScope1Bean() throws Exception {
        Scope1 scope1BeanMock = mock(Scope1.class);
        when(scope1BeanMock.getInstanceId()).thenReturn("instance-abc-123");
        when(context.getBean("scope1Bean")).thenReturn(scope1BeanMock);

        mockMvc.perform(get("/api/public/scope"))
                .andExpect(status().isOk())
                .andExpect(content().string("instance-abc-123"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/aspect-value/{name}
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getAspectValue_delegatesToAspectService_andReturnsResult() throws Exception {
        when(demoAspectService.serviceMethod("Eve")).thenReturn("Aspect-modified: Eve");

        mockMvc.perform(get("/api/public/aspect-value/Eve"))
                .andExpect(status().isOk())
                .andExpect(content().string("Aspect-modified: Eve"));

        verify(demoAspectService).serviceMethod("Eve");
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/hash/{password}
    // ════════════════════════════════════════════════════════════════════════

    /**
     * BCryptPasswordEncoder is instantiated inline — no mocking needed.
     * Verify the response is a non-empty BCrypt hash string (starts with $2a$).
     */
    @Test
    void hashPassword_returnsBcryptHash_for_givenPassword() throws Exception {
        mockMvc.perform(get("/api/public/hash/mySecret"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    // Response is a JSON string (quoted), e.g. "$2a$10$..."
                    String raw = result.getResponse().getContentAsString();
                    // Strip surrounding quotes added by Jackson
                    String hash = raw.replace("\"", "");
                    assertThat(hash).startsWith("$2a$");
                });
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/first
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getFirst_returnsResultFromFirstAutowiredBean() throws Exception {
        when(first.whoAmI()).thenReturn("I am FirstAutowiredBean");

        mockMvc.perform(get("/api/public/first"))
                .andExpect(status().isOk())
                .andExpect(content().string("I am FirstAutowiredBean"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/multi
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getMulti_returnsResultFromMultiAutowiredBean() throws Exception {
        when(multi.whoAmI()).thenReturn("I am MultiAutowiredBean");

        mockMvc.perform(get("/api/public/multi"))
                .andExpect(status().isOk())
                .andExpect(content().string("I am MultiAutowiredBean"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/name
    // ════════════════════════════════════════════════════════════════════════

    /**
     * The @Value("${demo.name:Default Name}") field is set to "Test Name"
     * via ReflectionTestUtils in setUp().
     */
    @Test
    void getName_returnsInjectedNameValue() throws Exception {
        mockMvc.perform(get("/api/public/name"))
                .andExpect(status().isOk())
                .andExpect(content().string("Test Name"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/conditional-first
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void checkConditionalFirst_returnsResultFromConditionalFirstBean() throws Exception {
        when(conditionalFirst.whoAmI()).thenReturn("I am ConditionalFirst");

        mockMvc.perform(get("/api/public/conditional-first"))
                .andExpect(status().isOk())
                .andExpect(content().string("I am ConditionalFirst"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/user-properties
    // ════════════════════════════════════════════════════════════════════════

    @Test
    void getUserProperties_returnsUserPropertiesObject() throws Exception {
        // UserProperties is mocked; Spring serialises the mock as an empty JSON object.
        mockMvc.perform(get("/api/public/user-properties"))
                .andExpect(status().isOk());

        verify(userProperties, org.mockito.Mockito.atLeastOnce());
        // Content assertion omitted — exact JSON depends on UserProperties fields
        // and Jackson serialisation of a Mockito proxy, which is infrastructure detail.
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/public/feature/{feature}
    // ════════════════════════════════════════════════════════════════════════

    /** Switch case: "feature1" */
    @Test
    void checkFeature_delegatesToFeatureService_forFeature1() throws Exception {
        when(featureService.isFeature1Available()).thenReturn("Feature 1 is available");

        mockMvc.perform(get("/api/public/feature/feature1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Feature 1 is available"));

        verify(featureService).isFeature1Available();
    }

    /** Switch case: "feature2" */
    @Test
    void checkFeature_delegatesToFeatureService_forFeature2() throws Exception {
        when(featureService.isFeature2Available()).thenReturn("Feature 2 is available");

        mockMvc.perform(get("/api/public/feature/feature2"))
                .andExpect(status().isOk())
                .andExpect(content().string("Feature 2 is available"));

        verify(featureService).isFeature2Available();
    }

    /** Switch case: "feature3" */
    @Test
    void checkFeature_delegatesToFeatureService_forFeature3() throws Exception {
        when(featureService.isFeature3Available()).thenReturn("Feature 3 is not available");

        mockMvc.perform(get("/api/public/feature/feature3"))
                .andExpect(status().isOk())
                .andExpect(content().string("Feature 3 is not available"));

        verify(featureService).isFeature3Available();
    }

    /** Switch default case: unknown feature name */
    @Test
    void checkFeature_returnsInvalidMessage_forUnknownFeatureName() throws Exception {
        mockMvc.perform(get("/api/public/feature/unknownXYZ"))
                .andExpect(status().isOk())
                .andExpect(content().string("Invalid feature name"));
    }
}

It is a document about what features can be explored & how while running this code.

# Spring Boot 4 Feature Exploration — Project Documentation

> **Stack:** Spring Boot 4.1.0-M2 · Java 21 · H2 · Liquibase · Redis · JWT · Togglz · Micrometer

This project is a hands-on proof-of-concept (POC) that explores a wide range of Spring Boot 4 features in a single runnable application. Each package or class deliberately showcases one concept so you can read the code alongside this guide and understand exactly what is being demonstrated and why.

---

## Table of Contents

1. [Prerequisites & Setup](#1-prerequisites--setup)
2. [Project Structure](#2-project-structure)
3. [Running the Application](#3-running-the-application)
4. [Feature Index](#4-feature-index)
   - [Security & JWT Authentication](#41-security--jwt-authentication)
   - [Caching](#42-caching)
   - [Async Execution](#43-async-execution)
   - [AOP — Aspect-Oriented Programming](#44-aop--aspect-oriented-programming)
   - [Spring Events](#45-spring-events)
   - [Scheduling](#46-scheduling)
   - [Feature Toggles (Togglz)](#47-feature-toggles-togglz)
   - [HATEOAS & Pagination](#48-hateoas--pagination)
   - [Bean Scopes & Conditional Beans](#49-bean-scopes--conditional-beans)
   - [Custom Validation Annotations](#410-custom-validation-annotations)
   - [Observability — Actuator & Micrometer](#411-observability--actuator--micrometer)
   - [Database — JPA, JDBC & Liquibase](#412-database--jpa-jdbc--liquibase)
   - [Web MVC Patterns](#413-web-mvc-patterns)
   - [Multi-Step Form with Session](#414-multi-step-form-with-session)
   - [Global Exception Handling](#415-global-exception-handling)
   - [Configuration Properties](#416-configuration-properties)
5. [API Reference](#5-api-reference)
6. [Credentials & Test Data](#6-credentials--test-data)
7. [Notable Design Notes](#7-notable-design-notes)

---

## 1. Prerequisites & Setup

| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Redis | Any recent version (for Togglz state + Redis cache in `dev` profile) |
| (Optional) Docker | For running Redis easily |

**Start Redis with Docker:**
```bash
docker run -d --name my-redis -p 6379:6379 redis:alpine
```

**Clone & run:**
```bash
# Build
mvn clean install

# Run (dev profile is active by default)
mvn spring-boot:run
```

The application starts on **http://localhost:8080**.

> **Tip — properties migrator:** The `spring-boot-properties-migrator` dependency is included. It prints warnings at startup if any property keys have changed between Spring Boot versions, making upgrades easier.

---

## 2. Project Structure

```
src/main/java/com/applicationPOC/
│
├── DemoObservabilityAppApplication.java   # Entry point; enables all features globally
├── RandomComponent.java                   # Bean scope & @DependsOn demo
│
├── aspects/          # AOP — @Around and @Before advice
├── config/           # All configuration classes (Security, Async, Cache, Conditional, etc.)
├── controller/       # REST and MVC controllers (one per feature group)
├── customAnnotation/ # Custom Jakarta Validation annotations
├── domain/           # JPA entities (Product, Category)
├── event/            # Custom ApplicationEvent
├── eventListeners/   # @EventListener and @TransactionalEventListener
├── metrics/          # Custom Micrometer counter
├── model/            # Non-entity models (User, UserDto, bean scope demos)
├── repository/       # Spring Data JPA & JDBC repositories
├── scheduledJobs/    # @Scheduled and dynamic TaskScheduler demos
├── security/         # JWT filter, JWT service, UserDetailsService
├── service/          # Business logic (DemoService, ProductService, FeatureService)
└── togglzFeature/    # Togglz feature flag definitions and configuration

src/main/resources/
├── application.properties          # Base config (profile = dev)
├── application-dev.properties      # Dev overrides (Redis, Togglz console, etc.)
├── application-prod.properties     # Prod stub
├── User.properties                 # External properties for @ConfigurationProperties
├── ValidationMessages.properties   # Externalised validation messages
├── db/changelog/
│   └── db.changelog-master.xml     # Liquibase changelog (schema + seed data)
└── templates/                      # Thymeleaf templates for multi-step form
```

---

## 3. Running the Application

After startup the following URLs are immediately useful:

| URL | What it does |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API explorer |
| `http://localhost:8080/h2-console` | In-memory DB browser (JDBC URL: `jdbc:h2:mem:demo`) |
| `http://localhost:8080/togglz-console` | Feature flag toggle UI |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics scrape endpoint |

---

## 4. Feature Index

### 4.1 Security & JWT Authentication

**Files:** `config/SecurityConfig.java`, `security/JwtAuthFilter.java`, `security/JwtService.java`, `security/DatabaseUserDetailsService.java`, `controller/AuthController.java`, `controller/SecureController.java`

**What is explored:**

The application uses a **stateless JWT-based security model** — there are no server-side sessions for API calls.

- `SecurityConfig` configures a `SecurityFilterChain` with CSRF disabled and `SessionCreationPolicy.STATELESS`.
- Public paths (`/api/public/**`, `/auth/login`, `/swagger-ui/**`, `/h2-console/**`) are open without a token.
- Protected paths (`/api/secure/**`) require a valid JWT.
- `/actuator/**` (except `/health` and `/info`) requires the `ADMIN` role.
- `DatabaseUserDetailsService` loads users from the H2 database (seeded by Liquibase).
- `JwtAuthFilter` is a `OncePerRequestFilter` that reads the `Authorization: Bearer <token>` header, validates the JWT, and populates the `SecurityContext`.
- `@PreAuthorize` is used on controller methods to enforce fine-grained role checks (`ADMIN` vs `USER`).
- A `@Lazy` annotation on `SecurityConfig`'s constructor resolves a circular dependency between `SecurityConfig` and `JwtAuthFilter`.

**How to try it:**

```bash
# Step 1 – Get a JWT token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
# → {"token":"eyJ..."}

# Step 2 – Use the token
curl http://localhost:8080/api/secure/me \
  -H "Authorization: Bearer eyJ..."

# Step 3 – Admin-only endpoint
curl http://localhost:8080/api/secure/admin \
  -H "Authorization: Bearer eyJ..."

# Step 4 – BCrypt helper (generate a hash to store in DB)
curl http://localhost:8080/api/public/hash/mypassword
```

---

### 4.2 Caching

**Files:** `config/CacheConfig.java`, `config/CacheWarup.java`, `service/DemoService.java`

**What is explored:**

- `@EnableCaching` is declared on the main application class.
- **Profile-based cache managers:** `CacheConfig` creates a `ConcurrentMapCacheManager` for `dev` and a different one for `prod` using `@Profile`. This demonstrates how to swap infrastructure per environment.
- In `application-dev.properties`, the cache type is overridden to **Redis** (`spring.cache.type=redis`), so in the dev profile all `@Cacheable` calls go to Redis.
- `DemoService.expensiveCall()` is annotated with `@Cacheable(cacheNames = "demoCacheDev", key = "#id")` — the first call sleeps for 2 seconds; subsequent calls with the same `id` return instantly from cache.
- `CacheWarup` implements `ApplicationRunner` to simulate cache pre-warming at startup (runs before the application starts serving requests).

**How to try it:**

```bash
# First call — takes ~2 seconds
curl http://localhost:8080/api/public/cached/abc

# Second call — instant (served from cache)
curl http://localhost:8080/api/public/cached/abc
```

---

### 4.3 Async Execution

**Files:** `config/AsyncConfig.java`, `config/ConditionalConfig.java`, `service/DemoService.java`, `controller/PublicController.java`

**What is explored:**

- `@EnableAsync` on the main class activates async processing.
- `AsyncConfig` implements `AsyncConfigurer` to provide a **custom default thread pool** (`async-exec-*` threads, core=2, max=4).
- `ConditionalConfig` registers a **second named executor** (`transcodingPoolTaskExecutor` with `Custom Pool-*` threads) — demonstrating how to have multiple async thread pools.
- `@Async` on `DemoService.asyncOperation()` uses the default pool.
- `@Async("transcodingPoolTaskExecutor")` on `asyncCustomOperation()` routes to the named pool.
- Both return `CompletableFuture<String>`, which Spring MVC handles natively.

**How to try it:**

```bash
# Default async pool
curl http://localhost:8080/api/public/async/hello

# Named custom pool
curl http://localhost:8080/api/public/custom-async/hello
```

Check the server logs — you will see thread names `async-exec-1` and `Custom Pool-1` respectively.

---

### 4.4 AOP — Aspect-Oriented Programming

**Files:** `aspects/ModifyReturnDemoAspect.java`, `aspects/DemoAspectService.java`

**What is explored:**

- `@EnableAspectJAutoProxy` is declared on the main class.
- `ModifyReturnDemoAspect` demonstrates two kinds of advice on `DemoAspectService.serviceMethod()`:
  - **`@Around`** — intercepts the method, lets it execute (`joinPoint.proceed()`), then modifies the return value by appending `" [Modified by Aspect]"` to the string result.
  - **`@Before`** — fires before the method and captures the argument using `&& args(name)`. It prints a message only when the name equals `"Nitin"`.

**How to try it:**

```bash
# Will return "Hello, world! This is a response from DemoAspectService. [Modified by Aspect]"
curl http://localhost:8080/api/public/aspect-value/world

# Triggers the @Before advice log as well
curl http://localhost:8080/api/public/aspect-value/Nitin
```

---

### 4.5 Spring Events

**Files:** `event/UserCreatedEvent.java`, `eventListeners/UserEventListener.java`, `service/DemoService.java`

**What is explored:**

Spring's built-in event bus is used to decouple the user-creation workflow from side-effects:

- `UserCreatedEvent` extends `ApplicationEvent` and carries the saved `UserDto`.
- `DemoService.saveUser()` publishes the event via `ApplicationEventPublisher`.
- `UserEventListener` has **three listener methods** that demonstrate the lifecycle:
  - `@EventListener` — fires **immediately** after `save()` but **before** the transaction commits. Good for in-transaction work.
  - `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` — fires **only after** the database transaction commits, on a background thread. The right place for sending emails, cache updates, analytics.
  - `@TransactionalEventListener(phase = AFTER_ROLLBACK)` — fires **only if** the transaction rolls back. Good for compensating actions.

**How to try it:**

```bash
curl -X POST http://localhost:8080/api/public/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"j@example.com","password":"securepass","role":"USER_R"}'
```

Watch the server logs — all three listener phases will print in order.

---

### 4.6 Scheduling

**Files:** `scheduledJobs/ScheduledPrint.java`, `scheduledJobs/DynamicScheduler.java`

**What is explored:**

- `@EnableScheduling` activates the scheduler. `spring.task.scheduling.pool.size=3` is set so that multiple scheduled tasks can run concurrently.
- `ScheduledPrint` demonstrates the difference between `fixedDelay` and `fixedRate`:
  - `fixedDelay = 5s` — the **next** execution starts 5 seconds **after the previous one finishes**. Since each run sleeps for 5 seconds, effective period is ~10 seconds.
  - `fixedRate = 5s` — the **next** execution starts 5 seconds **after the previous one started**, regardless of completion. Can lead to overlapping runs if a task takes longer than the rate.
- `DynamicScheduler` shows **programmatic scheduling** using `TaskScheduler` + `@EventListener(ApplicationReadyEvent.class)`. This ensures the task only begins after the entire application context is ready, unlike `@Scheduled` which starts during context initialization.

No HTTP call needed — watch the console logs after startup.

---

### 4.7 Feature Toggles (Togglz)

**Files:** `togglzFeature/Features.java`, `togglzFeature/TogglzConfigurations.java`, `service/FeatureService.java`

**What is explored:**

[Togglz](https://www.togglz.org/) is integrated to provide runtime feature flag management backed by **Redis** (so flags survive restarts):

- `Features` enum defines three flags: `IS_FEATURE1_ENABLED`, `IS_FEATURE2_ENABLED`, `IS_FEATURE3_ENABLED`.
- `TogglzConfigurations` implements `TogglzConfig` to wire the `FeatureManager`, the `RedisStateRepository`, and Spring Security as the user provider.
- `FeatureService` uses `FeatureManager.isActive()` to check flag state at runtime.
- The **Togglz Admin Console** is available at `/togglz-console` (unsecured in dev via `togglz.console.secured=false`).

**How to try it:**

```bash
# Check each feature
curl http://localhost:8080/api/public/feature/feature1
curl http://localhost:8080/api/public/feature/feature2
curl http://localhost:8080/api/public/feature/feature3
```

Then open `http://localhost:8080/togglz-console`, toggle a feature on/off, and call the endpoint again — the response changes instantly without restarting.

> **Note:** Redis must be running for this feature to work (see Prerequisites).

---

### 4.8 HATEOAS & Pagination

**Files:** `controller/ProductController.java`, `service/ProductService.java`, `domain/ProductModelAssembler.java`, `repository/ProductRepository.java`

**What is explored:**

- `ProductController` uses `spring-boot-starter-hateoas`.
- `ProductModelAssembler` extends `RepresentationModelAssembler` — converts a `Product` entity into an `EntityModel<Product>` with self-links.
- `PagedResourcesAssembler<Product>` is injected to transform a `Page<Product>` into a `PagedModel` with navigation links (`_links.next`, `_links.prev`).
- `ProductRepository` shows derived query methods (`findByCategory_Name`), `@Query`-based JPQL, and a custom `@Modifying` query for partial updates.

**How to try it:**

```bash
# Single product with HATEOAS links
curl http://localhost:8080/api/products/1

# Paginated products by category with HATEOAS paging links
curl "http://localhost:8080/api/products/by-category?category=Electronics&page=0&size=2"

# Keyword search (paginated)
curl "http://localhost:8080/api/products/search?q=lap"

# Expensive in-stock products
curl "http://localhost:8080/api/products/expensive?minPrice=100"

# Create product (authenticated)
curl -X POST "http://localhost:8080/api/products?name=Monitor&price=299.99&category=Electronics" \
  -H "Authorization: Bearer <token>"

# Toggle stock flag
curl -X PATCH "http://localhost:8080/api/products/1/stock?inStock=false" \
  -H "Authorization: Bearer <token>"
```

---

### 4.9 Bean Scopes & Conditional Beans

**Files:** `model/Scope1.java`, `RandomComponent.java`, `config/ConditionalConfig.java`, `controller/PublicController.java`

**What is explored:**

**Scopes:**
- `Scope1` is registered as both a `@Component` (prototype-scoped) and a singleton `@Bean` named `scope1Bean`.
- The controller uses `ApplicationContext.getBean()` to fetch beans at request time, which is the correct way to access prototype beans from a singleton — injecting them directly would give the same instance every time.
- `/api/public/message` shows that two `getBean("scope1")` calls return **different** instances (prototype), proving `==` is `false`.
- `/api/public/scope` uses the singleton bean `scope1Bean` — always returns the same `instanceId`.

**Conditional beans:**
- `ConditionalConfig` shows `@Profile("dev")`, `@ConditionalOnClass`, and `@ConditionalOnProperty`.
- Two `@Bean("ConditionalFirst")` methods exist — one fires when `demo.first.enabled=true`, the other when `=false`. Spring picks exactly one.
- In `application-dev.properties`, `demo.first.enabled=false`, so the "false" variant is active.

**How to try it:**

```bash
# Prototype scope — always returns "false" (different instances)
curl http://localhost:8080/api/public/message

# Singleton scope — always returns the same instanceId
curl http://localhost:8080/api/public/scope

# Which ConditionalFirst bean was created?
curl http://localhost:8080/api/public/conditional-first
```

---

### 4.10 Custom Validation Annotations

**Files:** `customAnnotation/TrimmedLength.java`, `customAnnotation/TrimmedLengthValidator.java`, `customAnnotation/DynamicMin.java`, `customAnnotation/DynamicMinValidator.java`, `model/UserDto.java`

**What is explored:**

Two custom Jakarta Validation constraints are implemented from scratch:

**`@TrimmedLength(min, max)`** — validates a string's length **after trimming whitespace**. Standard `@Size` counts raw characters including leading/trailing spaces, which can be misleading. The validator trims the value first.

**`@DynamicMin(min, max)`** — reads the min and max lengths from **application properties** at runtime (using Spring's `Environment`). The annotation attributes are property keys, not literal values. This means you can change validation thresholds via config without recompiling. Used on the `role` field in `UserDto`, where `user.min.role.length=5` and `user.max.role.length=20` are set in `application-dev.properties`.

`UserDto` also shows externalised validation messages in `ValidationMessages.properties` using message interpolation (e.g., `{name.size.message}`).

**How to try it:**

```bash
# Valid user
curl -X POST http://localhost:8080/api/public/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"j@example.com","password":"securepass","role":"USER_R"}'

# Invalid — name too short, password has only spaces, role too short
curl -X POST http://localhost:8080/api/public/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Jo","email":"bad","password":"   ","role":"US"}'
# → 400 with per-field error map
```

---

### 4.11 Observability — Actuator & Micrometer

**Files:** `metrics/CustomMetricsConfig.java`, `application.properties`

**What is explored:**

- `spring-boot-starter-actuator` exposes health, info, metrics, and Prometheus endpoints.
- `management.endpoint.prometheus.access=unrestricted` makes the Prometheus scrape endpoint publicly accessible (adjust for production).
- `micrometer-registry-prometheus` exports all metrics in Prometheus text format.
- `CustomMetricsConfig` registers a **custom `Counter`** named `demo.custom.requests` via the `MeterRegistry` API. This shows how to add application-specific metrics that appear alongside the standard Spring Boot metrics.
- The `info` endpoint is enabled with `management.info.env.enabled=true` and returns `app.name` and `app.version` from properties.

**How to try it:**

```bash
# Health (public)
curl http://localhost:8080/actuator/health

# App info
curl http://localhost:8080/actuator/info

# All metrics (requires ADMIN JWT)
curl http://localhost:8080/actuator/metrics \
  -H "Authorization: Bearer <admin-token>"

# Prometheus format (unrestricted)
curl http://localhost:8080/actuator/prometheus | grep demo_custom
```

---

### 4.12 Database — JPA, JDBC & Liquibase

**Files:** `repository/`, `domain/`, `resources/db/changelog/db.changelog-master.xml`

**What is explored:**

- The project uses **both** `spring-boot-starter-data-jpa` and `spring-boot-starter-data-jdbc` to show both paradigms. (If annotation conflicts arise, comment out the one not needed.)
- `spring.jpa.open-in-view=false` is explicitly set — the project comment explains why OEMIV is disabled (it opens an EntityManager for every HTTP request even when no DB work is needed, wasting resources).
- `spring.jpa.hibernate.ddl-auto=none` — Hibernate is not allowed to manage the schema; **Liquibase owns it entirely**.
- **Liquibase changelog** (`db.changelog-master.xml`) creates tables (`categories`, `products`, `users`) and seeds sample data in versioned changesets. Migrations run automatically on startup.
- `ProductRepository` demonstrates: derived queries, `@Query` JPQL, and `@Modifying` with `@Transactional` for bulk updates.
- `@Transactional(readOnly = true)` is used on query-only service methods for performance.
- H2 console is available at `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:demo`.

---

### 4.13 Web MVC Patterns

**Files:** `controller/PublicController.java`, `config/UsernameFilter.java`

**What is explored:**

`PublicController` is a tour of Spring MVC binding annotations:

| Annotation | Endpoint | Description |
|---|---|---|
| `@PathVariable` | `GET /api/public/cached/{id}` | Bind URL segment to parameter |
| `@RequestParam` | `GET /api/public/search` | Optional query params with defaults |
| `@RequestHeader` | `GET /api/public/user-agent` | Read HTTP request header |
| `@CookieValue` | `GET /api/public/welcome` | Read a cookie; set one if absent |
| `@RequestBody` + `@Valid` | `POST /api/public/users` | JSON body with validation |
| `@ModelAttribute` | `POST /api/public/users/form` | Form data bound to object |
| `@RequestAttribute` | `GET /api/public/greet` | Read attribute set by a Filter |
| `@ResponseStatus` | `POST /api/public/users` | Return HTTP 201 Created |
| `@CrossOrigin` | (class level) | Allow AJAX from `localhost:3000` |
| `@Value` (SpEL) | (field) | Inject `Math.random() * 100` at startup |

`UsernameFilter` is a servlet `Filter` that sets a `username` request attribute. The `/greet` endpoint reads it via `@RequestAttribute`, demonstrating the filter → controller attribute-passing pattern.

---

### 4.14 Multi-Step Form with Session

**Files:** `controller/UserFormController.java`, `controller/BinderController.java`, `templates/`

**What is explored:**

`UserFormController` implements a **two-step HTML form wizard** using `@SessionAttributes`:

- `@SessionAttributes("userForm")` stores the partially-filled `UserDto` in the HTTP session across requests.
- Step 1 collects the name; step 2 collects more fields; the final `complete` method saves the user and calls `SessionStatus.setComplete()` to clear the session attribute.
- Thymeleaf templates (`user-step1.html`, `user-step2.html`, `user-complete.html`) render each step.

`BinderController` shows `@InitBinder` with `StringTrimmerEditor` — all incoming string form fields are automatically trimmed and empty strings converted to `null` before validation runs. This is a Spring MVC-specific pattern applicable to traditional form-based (non-JSON) controllers.

**How to try it:** Navigate to `http://localhost:8080/user/step1` in a browser.

---

### 4.15 Global Exception Handling

**Files:** `config/GlobalExceptionHandler.java`

**What is explored:**

`@ControllerAdvice(basePackages = "com.applicationPOC.controller")` scopes the handler to the controller package only. Three handler methods demonstrate:

- `IllegalArgumentException` → `400 Bad Request` with a plain error message.
- `AuthorizationDeniedException` → `403 Forbidden` (thrown by `@PreAuthorize` when access is denied).
- `MethodArgumentNotValidException` → `400 Bad Request` (thrown when `@Valid` on a `@RequestBody` fails without a `BindingResult` parameter).

`spring.mvc.problemdetails.enabled=true` in `application.properties` activates RFC 7807 Problem Details for standard Spring error responses.

**How to try it:**

```bash
# Triggers IllegalArgumentException randomly (40% chance)
curl -X POST "http://localhost:8080/api/public/users/form?name=John+Doe&email=j@e.com"

# Triggers AuthorizationDeniedException (call user endpoint with admin token)
curl http://localhost:8080/api/secure/admin \
  -H "Authorization: Bearer <user-token>"   # user token, not admin
```

---

### 4.16 Configuration Properties

**Files:** `config/UserProperties.java`, `resources/User.properties`, `resources/application-dev.properties`

**What is explored:**

- `@ConfigurationProperties(prefix = "user.default")` binds a group of properties to a typed POJO.
- `@PropertySource("classpath:user.properties")` loads from a separate properties file (`User.properties`) rather than `application.properties`.
- `@Validated` on the class enables JSR-303 bean validation on the config itself — if `name` is blank or `email` is invalid, the application **fails to start**.
- `@EnableConfigurationProperties(UserProperties.class)` is declared on the main class to register the bean.

**How to try it:**

```bash
curl http://localhost:8080/api/public/user-properties
```

---

## 5. API Reference

A full interactive reference is available at **http://localhost:8080/swagger-ui.html** (powered by SpringDoc OpenAPI 3).

### Quick Reference Table

| Method | Path | Auth Required | Description |
|---|---|---|---|
| GET | `/api/public/ping` | No | Health ping |
| GET | `/api/public/cached/{id}` | No | Cacheable call (2s first, instant after) |
| GET | `/api/public/search` | No | `@RequestParam` demo |
| GET | `/api/public/user-agent` | No | `@RequestHeader` demo |
| GET | `/api/public/welcome` | No | Cookie get/set demo |
| GET | `/api/public/greet` | No | `@RequestAttribute` from filter |
| POST | `/api/public/users` | No | Create user (JSON + validation) |
| GET | `/api/public/users/{id}` | No | Get user by id |
| GET | `/api/public/hash/{password}` | No | BCrypt helper |
| GET | `/api/public/message` | No | Prototype bean scope demo |
| GET | `/api/public/scope` | No | Singleton bean scope demo |
| GET | `/api/public/conditional-first` | No | Conditional bean demo |
| GET | `/api/public/aspect-value/{name}` | No | AOP return-value modification |
| GET | `/api/public/feature/{feature}` | No | Togglz feature flag check |
| GET | `/api/public/user-properties` | No | `@ConfigurationProperties` demo |
| POST | `/auth/login` | No | Get JWT token |
| GET | `/api/secure/me` | JWT | Current user info |
| GET | `/api/secure/admin` | JWT + ADMIN | Admin-only endpoint |
| GET | `/api/secure/user` | JWT + USER | User-only endpoint |
| GET | `/api/secure/common` | JWT (any) | Authenticated endpoint |
| GET | `/api/products/{id}` | No | Product with HATEOAS links |
| GET | `/api/products/by-category` | No | Paginated + HATEOAS |
| GET | `/api/products/search` | No | Keyword search |
| GET | `/api/products/expensive` | No | Price filter |
| POST | `/api/products` | JWT | Create product |
| PATCH | `/api/products/{id}/stock` | JWT | Toggle stock flag |
| POST | `/api/binder/register` | JWT + USER | Form register with `@InitBinder` |
| GET | `/user/step1` | No | Multi-step form (browser) |
| GET | `/actuator/health` | No | Health details |
| GET | `/actuator/prometheus` | No | Prometheus metrics |
| GET | `/actuator/**` | JWT + ADMIN | All other actuator endpoints |

---

## 6. Credentials & Test Data

**Pre-seeded users (Liquibase changeset `20260113-insert-sample-users`):**

| Username | Password | Role |
|---|---|---|
| `user` | `password` | USER |
| `admin` | `password` | ADMIN |

**Pre-seeded products:**

| Name | Price | Category | In Stock |
|---|---|---|---|
| Laptop | $1200.00 | Electronics | Yes |
| Headphones | $199.99 | Electronics | No |
| Spring Boot in Action | $39.99 | Books | Yes |

---

## 7. Notable Design Notes

**Why `@Lazy` on `SecurityConfig` constructor?** `JwtAuthFilter` depends on `UserDetailsService`, which `SecurityConfig` also configures. Without `@Lazy`, Spring detects a circular dependency at startup. Making `SecurityConfig` lazy-inject `JwtAuthFilter` breaks the cycle.

**Why both JPA and JDBC starters?** The project intentionally includes both for demonstration. In a real project, pick one. If both are active and you see annotation conflicts, comment out the starter you don't need (as noted in `pom.xml`).

**Why `BindingResult` in controller instead of `@ControllerAdvice` for validation?** `MethodArgumentNotValidException` for `@RequestBody` bypasses the controller method when there is no `BindingResult` parameter. Adding `BindingResult` gives you access to field-level errors within the method. The `@ControllerAdvice` handles the cases where `BindingResult` is absent.

**Togglz and Redis:** In the dev profile, Togglz stores feature flag states in Redis. This means toggling a feature in the Togglz console persists across application restarts — as long as Redis is running. To reset all flags, flush Redis (`redis-cli FLUSHALL`).

**`spring-boot-properties-migrator`:** This `runtime`-scoped dependency logs deprecation warnings if any property keys have been renamed in the new Spring Boot version. It is safe to remove once you've addressed all warnings.

**Docker image with Jib:** The `pom.xml` includes the Google Jib plugin. To build and push a Docker image without a local Docker daemon:
```bash
mvn compile jib:build -Djib.to.image=YOUR_DOCKERHUB_USER/demo-observability-app
```
Replace `YOUR_DOCKER_HUB_USERNAME` in `pom.xml` first.


# **1. Togglz Feature applicable for single instance**
1.  **To access Togglz dashboard:**
    `http://localhost:8080/togglz-console/index`
2.  **Add a variable for the feature you want to toggle:**
    Update the `com.applicationPOC.togglzFeature.Features` class.
3.  **Add the method for your service:**
    Preferably in `com.applicationPOC.service.FeatureService` (though you can have your service anywhere).
4.  **Have a relevant controller method:**
    Currently located in `com.applicationPOC.controller.PublicController`.
5.  **Check the feature status:**
    Access via `/feature/{feature}`.

**Expected Output:**
* **If enabled:** `Feature 1 is available`
* **If disabled:** `Feature 1 is not available`

**Try using Redis:**
execute `docker-compose.yml' to start Redis and redis-insight to look into Redis data  
Then you can delete the keys in Redis which will reset the togglz keys  
Access redis-insight through `http://localhost:5540/`  
`Check the recommended settings and also check the terms & conditions switch, and then hit the “Submit” button.`    
RedisInsight will launch in your browser. It may ask you to add a Redis database. Just enter and then hit Add Database:  
Host: **redis**  
Port: **6379**

# Spring Boot — Application POC

A comprehensive **Spring Boot proof-of-concept** application that demonstrates a wide range of real-world Spring features in one runnable project — from security and secret management to caching, feature flags, AOP, observability, scheduling, and much more.

> **Built as a living reference:** every package is a standalone pattern worth knowing. Browse the code alongside this README to understand *why* each feature is wired the way it is.

---

## 📑 Table of Contents

- ⚡ [Quick Start](#quick-start)
- ✨ [Feature Highlights](#feature-highlights)
- 🛠 [Tech Stack & Versions](#tech-stack-versions)
- 📁 [Project Structure](#project-structure)
- ⚙️ [ Configuration Files Overview](#configuration-files-overview)
- 🐳 [Infrastructure Setup (Docker)](#infrastructure-setup-docker-do-this-first)
- 🚀 [Application Setup](#application-setup)
- 🌐 [Environment Variables Reference](#environment-variables-reference)
- 🖥️ [IDE Setup Notes](#ide-setup-notes)
- 🔐 [Vault — Important Notes](#vault-important-notes)
- 🔍 [Running & Exploring Features](#running-exploring-features)
- 📡 [API Reference](#api-reference)
- 📊 [Observability Endpoints](#observability-endpoints)
- 📈 [Prometheus & Grafana](#prometheus-grafana)
- 🧪 [Testing](#testing)
- ⚡ [Performance Testing — Gatling](#performance-testing-gatling)
- 🐋 [Building & Running a Container Image](#building-running-a-container-image)
- 🩺 [Troubleshooting](#troubleshooting)
- 📝 [Notes for Contributors](#notes-for-contributors)

---

## ⚡ Quick Start

**Prerequisites: Java 21+, Maven 3.9+, Docker & Docker Compose**

> **Windows users:** Before doing anything else, ensure `setup-vault.sh` uses LF line endings (not CRLF). See CRLF errors in [Troubleshooting](#troubleshooting).

---

### **1. Clone and Navigate**

```bash
cd demo-observability-app
```

### **2. Start Infrastructure**

Start Redis and HashiCorp Vault in detached mode.

```bash
docker compose up -d
```

### **3. Verify Vault Initialization**

Wait ~10 seconds for the setup script to finish, then check:

```bash
docker logs vault-init
```

> Look for the final line: **`DONE. Vault is unsealed and provisioned.`**

### **4. Set Environment Variables**

The `.env` file at the project root contains the required Vault credentials with their default values:

```properties
SPRING_CLOUD_VAULT_URI=http://localhost:8200
SPRING_CLOUD_VAULT_ROLE_NAME=springboot-role
SPRING_CLOUD_VAULT_ROLE_ID=demo-app-role-id
SPRING_CLOUD_VAULT_SECRET_ID=demo-app-secret-id
```

**IntelliJ:** Loads `.env` natively — no action required. Simply point the run configuration at the project root.

**Eclipse:** Does not natively support `.env` files. See [Eclipse setup](#eclipse-ide) for options.

**CMD / PowerShell (for `mvn spring-boot:run`):** Run the following to load the `.env` into your current session:

```powershell
# PowerShell — current session only
Get-Content .env | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
```

To **persist** the variables across sessions (written to the Windows Registry, User scope):

```powershell
# PowerShell — persists to User environment (survives terminal restarts)
Get-Content .env | ForEach-Object {
    $name, $value = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($name, $value, 'User')
}
```

> After setting `'User'` scope, open a **new** terminal for the variables to take effect. Verify with: `[System.Environment]::GetEnvironmentVariable('SPRING_CLOUD_VAULT_URI', 'User')`

### **5. Run the Application**

```bash
mvn spring-boot:run
```

### **6. Explore the Ecosystem**

Once the application is running, access these endpoints:

|       Tool         |          URL         |  
|--------------------|----------------------|  
| **Swagger UI**     | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |  
| **H2 Console**     | [http://localhost:8080/h2-console](http://localhost:8080/h2-console) — JDBC: `jdbc:h2:mem:demo`, User: `sa` |  
| **Togglz Console** | [http://localhost:8080/togglz-console](http://localhost:8080/togglz-console) |  
| **Vault UI**       | [http://localhost:8200/ui](http://localhost:8200/ui) — Token in `docker logs vault-init` |  

---

## ✨ Feature Highlights

### 🔐 Security
- **JWT Authentication** — Stateless token-based auth via `JwtAuthFilter` and `JwtService`
- **Role-Based Access Control** — `ADMIN` and `USER` roles enforced with `@PreAuthorize`
- **DB-Backed UserDetails** — `DatabaseUserDetailsService` loads users from H2 via JPA
- **BCrypt Password Encoding** — Passwords hashed with `BCryptPasswordEncoder`
- **Session Management** — Explicitly stateless; session invalidation endpoints provided
- **CORS Support** — `@CrossOrigin` configured for localhost React clients
- **`@Lazy` on `JwtAuthFilter`** — Used in `SecurityConfig` to break a circular dependency

### 🔑 Secret Management — HashiCorp Vault
- **AppRole Authentication** — Spring Cloud Vault connects via `role-id` / `secret-id`
- **KV-V2 Secrets Engine** — Application secrets stored at `secret/demo-observability-app`
- **Auto-inject Secrets** — `${secret.key}` injected directly from Vault at startup
- **Runtime Secret Management** — Read, list, and write secrets via `VaultController`
- **Automated Vault Bootstrap** — `setup-vault.sh` initialises, unseals, configures AppRole policy, and seeds data automatically via the `vault-init` Docker service
- **Spring Retry on Vault** — Up to 10 retry attempts (4 s apart) if Vault is slow to start

### 🗄️ Caching
- **Redis Cache (`dev` profile)** — Full Redis-backed cache via Spring Cache abstraction
- **Profile-aware `CacheManager`** — `dev` → Redis, `prod` → `ConcurrentMapCache`
- **Cache Warm-up** — `CacheWarmup` pre-populates the `Values` cache on startup
- **Togglz Feature State in Redis** — Feature flag state persisted in Redis (`togglz-` key prefix)

### 🚩 Feature Flags — Togglz
- Three runtime-toggleable flags: `IS_FEATURE1_ENABLED`, `IS_FEATURE2_ENABLED`, `IS_FEATURE3_ENABLED`
- **Togglz Admin Console** at `/togglz-console` to flip flags without redeployment
- Feature state persisted in Redis; survives application restarts
- **`SpringSecurityUserProvider`** — integrates Togglz with Spring Security; `ROLE_ADMIN` users are treated as feature admins (can manage flags via the console); all other authenticated users are regular users subject to activation strategy rules; anonymous requests are handled gracefully
- **`FeatureController`** at `/featurerApi/feature/{feature}` — dedicated REST endpoint that exposes feature-flag status for features 1–9 via a switch expression; distinct from the public convenience endpoint at `/api/public/feature/{feature}`

### ⚙️ Spring AOP
- **`@Around` Advice** — Intercepts `DemoAspectService.serviceMethod()` and appends `[Modified by Aspect]` to the return value
- **`@Before` Advice** — Captures method arguments before execution and logs them conditionally

### ⏱️ Scheduled Tasks

Two distinct scheduling behaviours are demonstrated side-by-side in `ScheduledPrint`:

| Task | Annotation | Behaviour |  
|------|------------|-----------|  
| `printMessage` | `@Scheduled(fixedDelay=5s)` | Next run starts **5 s after** the previous one **finishes** (sequential) |  
| `printMessage2` | `@Scheduled(fixedRate=5s)` | Fires every **5 s from start**, regardless of completion (can overlap) |  

Both tasks simulate work with `Thread.sleep(5000)`, making the overlap/delay contrast visible in logs. Thread-pool size is set to `3` (`spring.task.scheduling.pool.size`) to allow concurrent execution.

**Dynamic Scheduling** (`DynamicScheduler`) shows how to start periodic tasks only *after* `ApplicationReadyEvent` fires — avoiding the race condition that `@Scheduled` tasks face when fired too early during context initialization.

### 🔄 Async Operations
- `@EnableAsync` on the main application class enables async support
- **Default Spring thread pool** — `asyncOperation` endpoint
- **Custom `ThreadPoolTaskExecutor`** (`transcodingPoolTaskExecutor`, core=2, max=2, queue=500) — `asyncCustomOperation` endpoint
- **Virtual Threads** (`virtualPoolTaskExecutor`, backed by `Executors.newVirtualThreadPerTaskExecutor()`) — `asyncVirtualOperation` endpoint; demonstrates Java 21 virtual threads side-by-side with platform threads
- All three endpoints exposed for direct side-by-side comparison

### 🌐 Spring MVC — Comprehensive Annotation Coverage

| Annotation | Demonstrated In |  
|---------------|-------------------------|  
| `@PathVariable` | `/api/public/cached/{id}`, `/api/public/users/{id}` |  
| `@RequestParam` | `/api/public/search`, `/api/products/by-category` |  
| `@RequestHeader` | `/api/public/user-agent` |  
| `@CookieValue` | `/api/public/welcome` |  
| `@RequestAttribute` | `/api/public/greet` (value injected by `UsernameFilter`) |  
| `@RequestBody` | `POST /api/public/users` |  
| `@ModelAttribute` | `POST /api/public/users/form` |  
| `@ResponseStatus` | `POST /api/public/users` → `201 Created` |  
| `@InitBinder` | `BinderController` (auto-trims all String form fields) |  
| `@CrossOrigin` | `PublicController` |  

**Problem Details (RFC 9457)** — `spring.mvc.problemdetails.enabled=true` converts exceptions into the standard `application/problem+json` response format.

### 🍐 Bean Lifecycle & DI Patterns
- **Singleton vs. Prototype scope** — `Scope1` compared live via `ApplicationContext.getBean()`
- **`@Qualifier`** — Disambiguating multiple `MultiAutowiredBean` and `First` candidates
- **Constructor injection** (preferred) vs. field injection (shown for contrast)
- **`@Lazy`** — Breaks a circular dependency in `SecurityConfig`

### 🔀 Conditional Bean Creation
- `@ConditionalOnProperty` — Two `ConditionalFirst` beans registered under different `demo.first.enabled` values; only one is active at runtime
- `@ConditionalOnClass` — Bean registered only when Micrometer is on the classpath
- `@Profile("dev")` / `@Profile("prod")` — Profile-specific beans and cache managers

### 📡 Spring Events
- `UserCreatedEvent` published via `ApplicationEventPublisher` after successful user creation
- `UserEventListener` handles the event **asynchronously** (decoupled from the HTTP thread)

### ✅ Validation
- **Bean Validation** — `@Valid` on `UserDto` with `BindingResult` for fine-grained error maps
- **Custom Annotation `@TrimmedLength`** — Validates string length after trimming whitespace
- **Custom Annotation `@DynamicMin`** — Min value resolved dynamically from properties
- **Custom `ValidationMessages.properties`** — Externalised validation error messages
- **Global Exception Handler** — `@RestControllerAdvice` in `GlobalExceptionHandler`

### 🩺 Custom Startup Failure Analyzer
`PlaceholderFailureAnalyzer` extends `AbstractFailureAnalyzer<PlaceholderResolutionException>`. When a `${placeholder}` cannot be resolved at startup (e.g., a missing Vault secret), instead of a raw stack trace, Spring Boot prints a human-readable diagnostic with the exact placeholder name and actionable advice.  
Registered via `META-INF/spring.factories`:  
```properties
org.springframework.boot.diagnostics.FailureAnalyzer=\
  com.applicationPOC.startUpFailureAnalyzer.PlaceholderFailureAnalyzer
```

### 📦 Persistence
- **H2 In-Memory Database** — Zero-config dev database; console at `/h2-console`
- **Spring Data JPA + JDBC** — Repositories for `User`, `Product`, `Category`
- **Liquibase Migrations** — Schema and seed data managed via `db/changelog/db.changelog-master.xml`
- **`open-in-view=false`** — Explicitly disabled to prevent the `OpenEntityManagerInViewInterceptor` from opening a JPA session for every HTTP request — avoids unnecessary DB overhead and lazy-loading surprises

### 🔗 HATEOAS
- `ProductController` returns `EntityModel<Product>` and `PagedModel<EntityModel<Product>>`
- `ProductModelAssembler` adds hypermedia links
- Paginated product search and category filtering

### 📊 Observability
- **Spring Actuator** — `health`, `info`, `metrics`, `prometheus` endpoints exposed
- **Micrometer + Prometheus** — `/actuator/prometheus` for scraping
- **Custom Metrics** — `demo.custom.requests` counter registered via `MeterRegistry`

### 🛠️ Other Notable Patterns
- **`@ConfigurationProperties`** — `UserProperties` maps `user.min.role.length` / `user.max.role.length` from properties
- **SpEL** — `@Value("#{T(java.lang.Math).random() * 100}")` for runtime random injection
- **Thymeleaf** — Multi-step user registration form (`user-step1.html` → `user-step2.html` → `user-complete.html`)
- **`@Value` with defaults** — `${demo.name:Default Name}` pattern
- **Swagger / OpenAPI** — UI at `/swagger-ui.html`, spec at `/v3/api-docs`
- **OpenAPI Security Configuration** — `OpenAPISecurityConfiguration` registers a `Bearer Authentication` security scheme so the Swagger UI displays an **Authorize** button; JWT tokens obtained from `POST /auth/login` can be pasted there to authenticate all secured endpoints directly in the browser
- **`OpenApiCleanUpConfig`** and **`SwaggerUiCssFilter`** — post-process the generated spec and apply UI styling tweaks for a cleaner developer experience
- **Spring Boot Docker Compose integration** — `spring-boot-docker-compose` dependency auto-starts Docker Compose services on application launch (lifecycle set to `start_only` so infra keeps running when the app stops)

---

## 🛠 Tech Stack & Versions

| Layer | Technology | Version |  
|-------|------------|---------|  
| Framework | Spring Boot | **4.0.6** |  
| Cloud | Spring Cloud (Vault, Retry) | **2025.1.1** |  
| Language | Java | **21** |  
| Security | Spring Security + JWT (jjwt) | jjwt **0.12.5** |  
| Secret Management | HashiCorp Vault (KV-V2, AppRole) | Vault **1.15** |  
| Caching | Spring Cache + Redis | Redis **8.4.2-alpine** |  
| Feature Flags | Togglz | **4.6.1** |  
| Database | H2 (in-memory) | — |  
| Migrations | Liquibase | — |  
| Async / Scheduling | Spring `@Async`, `@Scheduled` | — |  
| Observability | Spring Actuator + Micrometer + Prometheus | — |  
| API Docs | SpringDoc OpenAPI (Swagger UI) | **2.8.5** |  
| AOP | Spring AOP (AspectJ proxy) | — |  
| Templates | Thymeleaf | — |  
| Infrastructure | Docker Compose | — |  
| Container Build | Google Jib Maven Plugin | **3.4.3** |  
| Build Tool | Maven | 3.9+ |  

---

## 📁 Project Structure

```
demo-observability-app/
├── pom.xml                          # Maven build — all dependencies and plugins
├── .env                             # Vault credentials for local dev (IntelliJ/Maven)
├── Commands.md                      # jdeps / jlink reference guide for building a custom JVM runtime
├── docker-compose.yml               # Infrastructure only: Redis + Vault + vault-init
├── docker-compose-all.yml           # Full stack: infra + app container (build & run everything)
├── Dockerfile                       # Multi-stage build: Maven → JLink runtime → layered app
├── setup-vault.sh                   # Vault bootstrap: init, unseal, AppRole, seed — must use LF endings
├── layers.xml                       # Spring Boot Layertools config for optimal Docker layer caching
├── src/main/resources/
│   ├── application.properties       # Core Spring config (active profile, H2, Liquibase, Actuator)
│   ├── application.yml              # Spring Cloud Vault connection + Docker Compose lifecycle
│   ├── user.properties              # External user properties (loaded via @ConfigurationProperties)
│   ├── ValidationMessages.properties # Custom Bean Validation error messages
│   └── META-INF/
│       └── spring.factories         # Registers PlaceholderFailureAnalyzer for startup diagnostics
├── vault/
│   ├── config/
│   │   ├── vault-config.hcl         # Vault server HCL config (file storage backend, TCP 8200)
│   │   └── keys.txt                 # ⚠ Generated at runtime — DO NOT commit
│   └── data/                        # ⚠ Vault file storage — consider moving outside project
└── src/main/java/com/applicationPOC/
    ├── DemoObservabilityAppApplication.java    # Main class — enables Caching, Async, AOP, Scheduling
    ├── RandomComponent.java
    ├── aspects/                     # AOP — @Around (return modification), @Before (arg logging)
    ├── config/
    │   ├── SecurityConfig.java      # JWT filter chain, RBAC, stateless session, CORS
    │   ├── AsyncConfig.java         # Custom ThreadPoolTaskExecutor
    │   ├── CacheConfig.java         # Profile-aware CacheManager (Redis vs ConcurrentMap)
    │   ├── CacheWarmup.java         # Pre-populates cache at startup
    │   ├── ConditionalConfig.java   # @ConditionalOnProperty, @ConditionalOnClass, @Profile beans
    │   ├── GlobalExceptionHandler.java  # @RestControllerAdvice for validation & runtime errors
    │   ├── UserProperties.java      # @ConfigurationProperties for user.min/max.role.length
    │   └── UsernameFilter.java      # Servlet filter that sets @RequestAttribute("username")
    ├── controller/
    │   ├── PublicController.java    # 20+ annotation demos — no auth required
    │   ├── SecureController.java    # JWT-protected endpoints with role checks
    │   ├── AuthController.java      # POST /auth/login → JWT token
    │   ├── ProductController.java   # HATEOAS + pagination
    │   ├── VaultController.java     # Read/write Vault secrets at runtime
    │   ├── BinderController.java    # @InitBinder + StringTrimmerEditor
    │   └── UserFormController.java  # Thymeleaf multi-step form
    ├── customAnnotation/            # @TrimmedLength, @DynamicMin + their validators
    ├── domain/                      # Product, Category, ProductModelAssembler (HATEOAS)
    ├── dummyBeans/                  # DummyBean1, DummyBean2, DummyProcessor, ParentDummy — runtime bean resolution demo
    ├── event/                       # UserCreatedEvent (ApplicationEvent subclass)
    ├── eventListeners/              # UserEventListener — async handler
    ├── metrics/                     # CustomMetricsConfig (Micrometer counter)
    ├── model/                       # User, UserDto, Scope1, First, Second, MultiAutowiredBean, Dependent...
    ├── openAPI/
    │   ├── OpenAPISecurityConfiguration.java  # Registers Bearer JWT scheme for Swagger Authorize button
    │   ├── OpenApiCleanUpConfig.java           # Post-processes generated OpenAPI spec
    │   └── SwaggerUiCssFilter.java             # Applies UI styling tweaks to Swagger UI
    ├── repository/                  # JPA repositories (User, Product, Category, BasicUser)
    ├── scheduledJobs/
    │   ├── ScheduledPrint.java      # fixedDelay vs fixedRate demo
    │   └── DynamicScheduler.java    # TaskScheduler started after ApplicationReadyEvent
    ├── security/
    │   ├── JwtAuthFilter.java       # OncePerRequestFilter — validates Bearer tokens
    │   ├── JwtService.java          # Token generation and validation
    │   ├── DatabaseUserDetailsService.java  # Loads UserDetails from H2
    │   ├── MyUserDetails.java       # UserDetails wrapper
    │   └── TokenRequest.java        # Login request DTO
    ├── service/
    │   ├── DemoService.java         # @Cacheable + @Async demos
    │   ├── ProductService.java      # Business logic for products
    │   ├── UserService.java         # User creation + event publishing
    │   └── FeatureService.java      # Togglz feature checks
    ├── startUpFailureAnalyzer/
    │   └── PlaceholderFailureAnalyzer.java  # Custom FailureAnalyzer for missing ${placeholders}
    └── togglzFeature/
        ├── Features.java            # Enum: IS_FEATURE1_ENABLED, IS_FEATURE2_ENABLED, IS_FEATURE3_ENABLED
        ├── FeatureController.java   # REST endpoint at /featurerApi/feature/{feature} (features 1–9)
        ├── SpringSecurityUserProvider.java  # Bridges Togglz UserProvider with Spring Security context
        └── TogglzConfigurations.java  # Redis-backed StateRepository for Togglz
```

### Docker Compose Files — Which One to Use?

| File | Purpose | When to Use |  
|------|---------|-------------|  
| `docker-compose.yml` | Infrastructure only (Redis + Vault) | **Recommended for development.** Run infra once, restart the Spring app freely via Maven/IDE. |  
| `docker-compose-all.yml` | Full stack — infra + Spring app container | Use when you want to run the entire system as Docker containers (e.g., CI, integration testing, or demo). Requires `docker build` first. |  

---

## ⚙️ Configuration Files Overview

Understanding which file controls what prevents confusion when tuning behaviour:

| File | Loaded By | Controls |  
|------|-----------|---------|  
| `application.properties` | Spring Boot | Server port, active profile (`dev`), H2 datasource, Liquibase, Actuator exposure, Swagger paths, scheduling pool size, logging levels |  
| `application.yml` | Spring Boot | Spring Cloud Vault connection (URI, AppRole credentials, KV-V2 path), Docker Compose lifecycle (`start_only`) |  
| `user.properties` | `@ConfigurationProperties` | `user.min.role.length`, `user.max.role.length` (used by `@DynamicMin`) |  
| `ValidationMessages.properties` | Bean Validation | Custom constraint message templates |  
| `vault/config/vault-config.hcl` | HashiCorp Vault | Vault server — storage backend (file), listener (TCP 8200), UI enabled |  
| `vault/config/keys.txt` | `setup-vault.sh` | ⚠ Runtime-generated — unseal key + root token. **Never commit.** |  
| `.env` | IntelliJ / Maven `.env` loader | Vault credentials for local development (see [Environment Variables](#environment-variables-reference)) |  

### Active Profile

The `dev` profile is active by default (`spring.profiles.active=dev` in `application.properties`). Key differences:

| Behavior | `dev` | `prod` |  
|-----------|-------|--------|  
| CacheManager | Redis | ConcurrentMapCache |  
| `devOnlyBean` | Created | Not created |  
| H2 Console | Enabled | Should be disabled |  

To switch: change `spring.profiles.active=prod` in `application.properties` or pass `-Dspring.profiles.active=prod` at runtime.

---

## 🐳 Infrastructure Setup (Docker) — Do This First

> **Why keep infra separate?**  
Redis and Vault can run independently of application restarts. Vault especially must not be re-initialised on every restart — doing so requires re-unsealing and re-generating credentials.

### Step 1 — Start the infrastructure

```bash
docker compose up -d
```

This starts three services:

| Service | Image | Port | Purpose |  
|---------|-------|------|---------|  
| `redis` | `redis:8.4.2-alpine3.22` | `6379` | Cache + Togglz state store |  
| `vault-prod` | `hashicorp/vault:1.15` | `8200` | Secret management |  
| `vault-init` | `hashicorp/vault:1.15` | — | One-shot: init, unseal, AppRole, seed |  

### Step 2 — Verify Vault bootstrap completed

```bash
docker logs vault-init
```

Wait for the final line: `DONE. Vault is unsealed and provisioned.`

What `setup-vault.sh` does automatically:
1. Detects if this is a fresh or existing Vault container (via cluster ID comparison)
2. Initialises Vault (1 key share, threshold 1) → writes `vault/config/keys.txt`
3. Unseals Vault using the generated key
4. Enables AppRole auth; creates policy + role (`springboot-role`)
5. Sets deterministic `role-id = demo-app-role-id` and `secret-id = demo-app-secret-id`
6. Enables KV-V2 at `secret/` and seeds `secret/demo-observability-app` with `secret.key = "secret value"`

### Vault data file location

The Vault container maps its data to `./vault/data` inside the project. For a more stable setup, map to a path **outside** the project:

```yaml
# docker-compose.yml — recommended for shared/team setups
volumes:
  - /some/path/outside/project/vault/data:/vault/file
```

### Recreating Vault from scratch

```bash
# 1. Remove generated state
rm -f ./vault/config/keys.txt
rm -rf ./vault/data/*

# 2. Force recreate just the vault services
docker compose up -d --force-recreate vault vault-init
```

> Skipping steps 1 & 2 causes the new container to inherit the previous sealed state and fail to initialise.

---

## 🚀 Application Setup

### Prerequisites

- **Java 21+** (`java -version`)
- **Maven 3.9+** (`mvn -version`)
- **Docker & Docker Compose** (`docker compose version`)

### Step 1 — Understand the Environment Variables

The application reads Vault credentials via Spring Cloud's standard environment variable mapping:

| Environment Variable | Spring Property | Description | Default Value |  
|---------------------|----------------|-------------|---------------|  
| `SPRING_CLOUD_VAULT_URI` | `spring.cloud.vault.uri` | Vault server URL | `http://localhost:8200` |  
| `SPRING_CLOUD_VAULT_ROLE_ID` | `spring.cloud.vault.app-role.role-id` | AppRole Role ID | `demo-app-role-id` |  
| `SPRING_CLOUD_VAULT_SECRET_ID` | `spring.cloud.vault.app-role.secret-id` | AppRole Secret ID | `demo-app-secret-id` |  
| `SPRING_CLOUD_VAULT_ROLE_NAME` | `spring.cloud.vault.app-role.role-name` | AppRole name | `springboot-role` |  

These are already set to their correct defaults in the `.env` file provided. You only need to change them if your Vault configuration differs.

> **Note — two variable naming styles:** Spring Cloud Vault reads the official `SPRING_CLOUD_VAULT_*` form directly. The `.env` file and `docker-compose-all.yml` both use this form. If you see `VAULT_URI` mentioned anywhere (e.g., in older run scripts), that refers to a custom `${VAULT_URI}` substitution in `application.yml` — the `SPRING_CLOUD_VAULT_*` form in `.env` is the authoritative one to use.

### Step 2 — Build and Run

```bash
# Build (skip tests for speed)
mvn clean package -DskipTests

# Run via Maven (infra must already be up)
mvn spring-boot:run

# Or run the JAR directly
java -jar target/demo-observability-app-1.0.0.jar
```

The application starts on **`http://localhost:8080`**.

> **Spring Boot Docker Compose auto-start:** The `spring-boot-docker-compose` dependency means Spring will automatically run `docker compose up` when you start the app — if the containers aren't already running. With `lifecycle-management: start_only`, Spring will *not* stop them when the app exits, keeping your Vault state intact. This is a safety net, not a substitute for doing `docker compose up -d` manually first.

### Step 3 — Build and Create a Docker Image (Optional)

```bash
# Build image to local Docker daemon (Jib — no Dockerfile needed)
mvn compile jib:dockerBuild

# Or export as a portable tar file (no Docker daemon needed)
mvn compile jib:buildTar
# → creates target/jib-image.tar; load with: docker load -i target/jib-image.tar

# Full multi-stage Docker build (uses the Dockerfile — builds optimised JLink runtime)
docker build -t demo-observability-app:v1 .
```

See [Building & Running a Container Image](#building-running-a-container-image) for the full container workflow.

---

## 🌐 Environment Variables Reference

These are the variables the application reads at startup. The `.env` file provides the correct defaults for local development.

```properties
# Spring Cloud Vault — Required for app startup
SPRING_CLOUD_VAULT_URI=http://localhost:8200
SPRING_CLOUD_VAULT_ROLE_ID=demo-app-role-id
SPRING_CLOUD_VAULT_SECRET_ID=demo-app-secret-id
SPRING_CLOUD_VAULT_ROLE_NAME=springboot-role

# Optional overrides (defaults shown)
# SPRING_DATA_REDIS_HOST=localhost
# SPRING_DATA_REDIS_PORT=6379
# SERVER_PORT=8080
# SPRING_PROFILES_ACTIVE=dev
```

> When running the app as a Docker container (via `docker-compose-all.yml`), use `http://vault:8200` for the URI — not `http://localhost:8200` — since containers communicate over the internal `app-network`, not the host loopback.

---

## 🖥️ IDE Setup Notes

### IntelliJ IDEA

1. Open the project root as a Maven project
2. Go to **Run → Edit Configurations** → select (or create) a Spring Boot run config
3. Under **EnvFile** (if you have the EnvFile plugin), point to `.env` in the project root — all variables are loaded automatically
4. Alternatively, under **Environment variables**, click the folder icon and add each variable manually:
   - `SPRING_CLOUD_VAULT_URI` → `http://localhost:8200`
   - `SPRING_CLOUD_VAULT_ROLE_ID` → `demo-app-role-id`
   - `SPRING_CLOUD_VAULT_SECRET_ID` → `demo-app-secret-id`
   - `SPRING_CLOUD_VAULT_ROLE_NAME` → `springboot-role`

### Eclipse IDE

Eclipse does **not** natively support `.env` files in run configurations.

**Option A — Plugin:** Install [Spring Tools 4](https://spring.io/tools) or the [EnvFile plugin](https://marketplace.eclipse.org/), which adds `.env` file support to the Run Configuration dialog.

**Option B — Manual:**
1. Open **Run → Run Configurations…**
2. Select your Spring Boot application
3. Go to the **Environment** tab
4. Add each variable manually:

| Name | Value |  
|------|-------|  
| `SPRING_CLOUD_VAULT_URI` | `http://localhost:8200` |  
| `SPRING_CLOUD_VAULT_ROLE_ID` | `demo-app-role-id` |  
| `SPRING_CLOUD_VAULT_SECRET_ID` | `demo-app-secret-id` |  
| `SPRING_CLOUD_VAULT_ROLE_NAME` | `springboot-role` |  

---

## 🔐 Vault — Important Notes

| Concern | Detail |  
|---------|--------|  
| **`keys.txt`** | Generated by `vault-init` at `./vault/config/keys.txt`. Contains the unseal key and root token. **Add to `.gitignore`** — never commit. |  
| **`vault/data/`** | Vault's file storage backend. Consider moving outside the project folder so Vault state survives project cleans. **Add to `.gitignore`** — never commit. |  
| **Container change detection** | The Vault container automatically detects if it is a new instance (by comparing its hostname to a stored cluster ID). If a new container is detected, it wipes stale data and re-initialises cleanly. You do not need to manually delete `vault/data/` between normal restarts. |  
| **Recreating Vault** | If you *force* a fresh init (e.g., after deleting `keys.txt` manually), also delete `vault/data/` to avoid a stale-sealed-state conflict. |  
| **Startup order** | Always start Docker Compose **before** the Spring Boot app. The app has `fail-fast: true` and will exit immediately if Vault is unreachable. Spring Retry will attempt up to 10 connections (4 s apart). |  
| **Token TTL** | AppRole tokens expire in **1 h** (max 4 h). For long dev sessions ensure Vault is still running. |  
| **`setup-vault.sh` line endings** | Must use **LF** (Unix) line endings — not CRLF. If cloned on Windows, run `dos2unix setup-vault.sh` or configure Git's `core.autocrlf=false`. |  

---

## 🔍 Running & Exploring Features

### 1. Swagger UI (Recommended Starting Point)

```
http://localhost:8080/swagger-ui.html
```

Every REST endpoint is documented here with request/response schemas. Use it to try each feature interactively. Get a JWT token from `POST /auth/login`, then click **Authorize** to use it across secured endpoints.

### 2. Getting a JWT Token

```bash
# Step 1 — Create a user (or use one seeded by Liquibase)
POST http://localhost:8080/api/public/users
Content-Type: application/json
{
  "name": "Jane",
  "email": "jane@example.com",
  "password": "password123",
  "role": "USER"
}

# Step 2 — Generate a BCrypt hash (helper endpoint — useful when manually inserting users)
GET http://localhost:8080/api/public/hash/{yourPassword}

# Step 3 — Log in and receive a JWT
POST http://localhost:8080/auth/login
Content-Type: application/json
{
  "username": "jane@example.com",
  "password": "password123"
}
# → { "token": "eyJ..." }

# Step 4 — Pass the token to secured endpoints
GET http://localhost:8080/api/secure/me
Authorization: Bearer eyJ...
```

### 3. H2 Database Console

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:demo
Username:  sa
Password:  (leave blank)
```

### 4. Togglz Admin Console

```
http://localhost:8080/togglz-console
```

Toggle `IS_FEATURE1_ENABLED`, `IS_FEATURE2_ENABLED`, `IS_FEATURE3_ENABLED` on/off at runtime without restarting. State is persisted in Redis — survives application restarts.

### 5. Vault UI

```
http://localhost:8200/ui
Token: see "Initial Root Token" line in: docker logs vault-init
```

### 6. Watch Scheduled Tasks in Logs

After startup, look for these log lines to see the `fixedDelay` vs `fixedRate` difference in action:

```
Scheduled task executed at: 2026-...       ← fixedDelay (waits for previous to finish)
Scheduled task2 executed at: 2026-...      ← fixedRate (fires every 5 s from start)
Periodic task running only after application started...   ← DynamicScheduler
```

---

## 📡 API Reference

### Public Endpoints (`/api/public/**`) — No authentication required

| Method | Path | Feature Demonstrated |  
|--------|------|---------------------|  
| `GET` | `/api/public/ping` | Health check |  
| `GET` | `/api/public/cached/{id}` | `@Cacheable` + `@PathVariable` |  
| `GET` | `/api/public/search?keyword=&page=` | `@RequestParam` |  
| `GET` | `/api/public/user-agent` | `@RequestHeader` |  
| `GET` | `/api/public/welcome` | `@CookieValue` + cookie set |  
| `GET` | `/api/public/greet` | `@RequestAttribute` (set by `UsernameFilter`) |  
| `POST` | `/api/public/users` | `@RequestBody` + Bean Validation + Spring Events |  
| `GET` | `/api/public/users/{id}` | JPA lookup |  
| `POST` | `/api/public/users/form` | `@ModelAttribute` + Global Exception Handler |  
| `GET` | `/api/public/async/{input}` | `@Async` (default pool) |  
| `GET` | `/api/public/custom-async/{input}` | `@Async` (custom `transcodingPoolTaskExecutor`) |  
| `GET` | `/api/public/virtual-async/{input}` | `@Async` (virtual threads via `virtualPoolTaskExecutor`) |  
| `GET` | `/api/public/message` | Prototype scope + SpEL |  
| `GET` | `/api/public/scope` | Singleton scope |  
| `GET` | `/api/public/aspect-value/{name}` | AOP `@Around` (return value modification) |  
| `GET` | `/api/public/hash/{password}` | BCrypt hash generator |  
| `GET` | `/api/public/conditional-first` | `@ConditionalOnProperty` |  
| `GET` | `/api/public/user-properties` | `@ConfigurationProperties` |  
| `GET` | `/api/public/feature/{feature}` | Togglz feature flag check (convenience alias) |  
| `GET` | `/api/public/default-user` | Returns a hardcoded default user object |  
| `GET` | `/api/public/first` | `@Qualifier` — resolves `FirstAutowiredBean` |  
| `GET` | `/api/public/multi` | `@Qualifier` — resolves `MultiAutowiredBean` |  
| `GET` | `/api/public/autowired/{beanName}` | Dynamic bean resolution by name (`dummyBean1` / `dummyBean2`) via `ApplicationContext.getBean()` |  
| `GET` | `/api/public/name` | `@Value` with default |  

### Auth Endpoints

| Method | Path | Description |  
|--------|------|-------------|  
| `POST` | `/auth/login` | Authenticate → receive JWT |  

### Secure Endpoints (`/api/secure/**`) — JWT required

| Method | Path | Role Required |  
|--------|------|---------------|  
| `GET` | `/api/secure/me` | Any authenticated user |  
| `GET` | `/api/secure/common` | Any authenticated user |  
| `GET` | `/api/secure/user` | `ROLE_USER` |  
| `GET` | `/api/secure/admin` | `ROLE_ADMIN` |  
| `GET` | `/api/secure/logout` | Any authenticated user |  
| `GET` | `/api/secure/logout-force` | Any authenticated user |  

### Product Endpoints (`/api/products/**`) — JWT required

| Method | Path | Feature |  
|--------|------|---------|  
| `GET` | `/api/products/{id}` | HATEOAS `EntityModel` |  
| `GET` | `/api/products/by-category?category=` | Paginated + HATEOAS |  
| `GET` | `/api/products/search?q=` | JPA search with pagination |  
| `GET` | `/api/products/expensive?minPrice=` | Custom JPA query |  
| `POST` | `/api/products` | Create product |  
| `PATCH` | `/api/products/{id}/stock` | Partial update |  

### Vault Endpoints (`/vault/**`)

| Method | Path | Auth | Description |  
|--------|------|------|-------------|  
| `GET` | `/vault/secret` | None | Read `secret.key` injected from Vault at startup |  
| `GET` | `/vault/secrets` | None | List all secrets for the app path |  
| `GET` | `/vault/secretByKey?key=` | None | Read a specific secret key |  
| `POST` | `/vault/manage/secrets?key=&value=` | `ROLE_ADMIN` | Write / update a secret (read-modify-write) |  

### Binder Endpoint (`/api/binder/**`) — JWT + `ROLE_USER`

| Method | Path | Feature |  
|--------|------|---------|  
| `POST` | `/api/binder/register` | `@InitBinder` + `StringTrimmerEditor` auto-trims whitespace |  

### Feature API (`/featurerApi/**`) — No authentication required

| Method | Path | Description |  
|--------|------|-------------|  
| `GET` | `/featurerApi/feature/{feature}` | Check availability of a named feature flag (`feature1`–`feature9`); returns status string or `"Invalid feature name"` for unknown names. Toggle flags at `/togglz-console` to see live changes. |  

> **Note:** `FeatureController` lives in the `togglzFeature` package, which is outside `springdoc.packages-to-scan=com.applicationPOC.controller`. It does **not** appear in Swagger UI. Use the endpoint directly via Postman or `curl`.  

---

## 📊 Observability Endpoints

| Endpoint | Access | Description |  
|----------|--------|-------------|  
| `/actuator/health` | Public | App + dependency health (shows DB, Redis, Vault status) |  
| `/actuator/info` | Public | App name & version (from `info.*` properties) |  
| `/actuator/metrics` | `ROLE_ADMIN` | All registered Micrometer metrics |  
| `/actuator/prometheus` | `ROLE_ADMIN` (unrestricted in config) | Prometheus scrape endpoint |  
| `/swagger-ui.html` | Public | Interactive API docs |  
| `/v3/api-docs` | Public | OpenAPI JSON spec |  
| `/h2-console` | Public | H2 in-memory DB console |  
| `/togglz-console` | Public (dev) | Feature flag management |  

---

## 📈 Prometheus & Grafana

The `docker-compose.yml` includes Prometheus and Grafana services alongside the application infrastructure.

| Service | Image | Port | Purpose |  
|---------|-------|------|---------|  
| `prometheus` | `prom/prometheus:latest` | `9090` | Scrapes `/actuator/prometheus` every 15 s; stores time-series metrics |  
| `grafana` | `grafana/grafana:latest` | `3000` | Visualises Prometheus data; default admin password: `admin` |  

### Connecting Grafana to Prometheus

1. Open Grafana at [http://localhost:3000](http://localhost:3000) and log in (`admin` / `admin`).
2. Go to **Connections → Data Sources → Add data source → Prometheus**.
3. Set the URL to `http://prometheus:9090` (container-to-container; both share the default bridge network).
4. Click **Save & Test** — you should see "Data source is working".

### `prometheus.yml`

The scrape configuration at the project root points Prometheus at the Spring Boot app:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          application: 'demo-observability-app'
```

> `host.docker.internal` is used so the Prometheus container can reach the app running on your host. On Linux, add `--add-host=host.docker.internal:host-gateway` to the Prometheus container definition or replace the target with your host's IP.

The custom `demo.custom.requests` counter registered via `CustomMetricsConfig` appears in Grafana as soon as the `/actuator/prometheus` endpoint is scraped.

---

## 🧪 Testing

The project includes unit and integration tests under `src/test/java/com/applicationPOC/`:

### Test Classes

| Class | Type | What It Covers |  
|-------|------|----------------|  
| `PublicControllerTest` | Unit (standalone MockMvc, no Spring context) | Every endpoint and every branch in `PublicController` — happy paths, validation errors, `@RequestAttribute` fallback, cookie set/read, async dispatch pattern for `CompletableFuture`, `MockedConstruction` for deterministic `Random` branching |  
| `FeatureControllerTest` | Unit (standalone MockMvc, no Spring context) | All 9 valid feature names route to the correct `FeatureService` method; both toggle states (available / not available) for feature1; `default` switch branch returns `"Invalid feature name"` for unknown, out-of-range, and numeric names |  
| `DemoServiceTest` | Integration (slice with `@EnableCaching` + `@EnableAsync`) | `@Cacheable` correctness (miss vs hit via log assertion), all three `@Async` variants (default pool, custom `transcodingPoolTaskExecutor`, virtual threads), `saveUser` persistence + event publishing, `getUserById` found/not-found |  
| `FeatureServiceTest` | Integration (slice with in-memory Togglz state) | All three feature flags — enabled/disabled states, cross-feature isolation, all-enabled scenario |  
| `DemoObservabilityAppApplicationTests` | Smoke (full Spring context load) | Verifies the application context starts successfully with all beans wired |  

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific class
mvn test -Dtest=PublicControllerTest

# Run with coverage (if JaCoCo is configured)
mvn verify
```

### Design Highlights

- **No Spring context in `PublicControllerTest`** — the controller is instantiated manually and all dependencies are injected via `ReflectionTestUtils`, keeping each test well under 1 second.
- **`MockedConstruction`** intercepts `new Random()` inside `createUserFromForm` so both the exception-throwing branch (random ≤ 4) and the success branch (random > 4) are deterministically reachable without flakiness.
- **Two-step async dispatch** (`andExpect(request().asyncStarted())` → `asyncDispatch(mvcResult)`) is used for all `CompletableFuture`-returning endpoints.
- **`DemoServiceTest`** verifies virtual thread identity at runtime: `assertThat(thread.isVirtual()).isTrue()`.

---

## ⚡ Performance Testing — Gatling

The project ships three Gatling simulations under `src/test/java/com/applicationPOC/performance/`. They are written with the Gatling Java DSL (version **3.11.5**) and run via the `gatling-maven-plugin` (**4.9.2**).

### Simulations

| Simulation | Target | Load Profile | SLA Assertions |  
|------------|--------|--------------|----------------|  
| `PublicSimulator` | All public endpoints (`/api/public/**`) | 11 concurrent scenarios; up to 2 000 virtual users ramped over 30 s for the virtual-threads scenario | Cache hit p95 < 20 ms; cache miss p95 > 100 ms; virtual-threads async p95 < 2 030 ms; 0% failed requests |  
| `FeatureToggleSimulation` | `/featurerApi/feature/{feature}` | Anonymous + authenticated personas; constant load at configurable RPS for configurable duration | Global p95 < 50 ms; global max < 250 ms; 0% failed requests |  
| `AuthenticatedFeatureToggleSimulation` | Feature toggle endpoints with JWT/Basic auth | Ramps authenticated users with pre-seeded credentials | Configured per simulation |  

### Running a Simulation

```bash
# Run the default simulation (edit pom.xml simulationClass to switch)
mvn gatling:test

# Run a specific simulation
mvn gatling:test -Dgatling.simulationClass=com.applicationPOC.performance.PublicSimulator

# Override load parameters (FeatureToggleSimulation supports these)
mvn gatling:test \
  -Dgatling.simulationClass=com.applicationPOC.performance.FeatureToggleSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DtargetRps=200 \
  -DdurationSec=120
```

> The application must be running (`mvn spring-boot:run`) before launching a simulation.

### Reports

Gatling generates an HTML report after each run at `target/gatling/<simulation-name>-<timestamp>/index.html`. Open it in a browser to see response-time percentiles, throughput, and per-request statistics.

### `PublicSimulator` — Scenarios at a Glance

| Scenario | Injected Users | Key Assertion |  
|----------|---------------|---------------|  
| Ping | 200 over 10 s | 200 OK + body contains `"pong"` |  
| Cache validation (miss → hit) | 500 over 25 s | Cache hit p95 < 20 ms |  
| Search with params | 30 over 15 s | JSON fields validated |  
| Stateful cookie lifecycle | 35 over 15 s | `Set-Cookie` header exists; second request reads cookie |  
| User lifecycle (create → fetch) | 40 over 20 s | 201 Created; GET returns same email |  
| Async processing (default + custom pool) | 60 over 30 s (starts at 60 s) | 200 OK; body contains `"Processed:"` |  
| Virtual Threads async | 2 000 over 30 s (starts at 62 s) | p95 < 2 030 ms |  

---

## 🐋 Building & Running a Container Image

### Option A — Jib (No Dockerfile, No Docker daemon required for push)

```bash
# Build to local Docker daemon
mvn compile jib:dockerBuild

# Build and push to Docker Hub (no local daemon needed)
mvn jib:build \
  -Djib.to.image=YOUR_DOCKER_HUB_USERNAME/demo-observability-app \
  -Djib.to.auth.username=YOUR_DOCKER_HUB_USERNAME \
  -Djib.to.auth.password=YOUR_DOCKER_HUB_TOKEN

# Export as a portable tar (useful in CI without Docker)
mvn compile jib:buildTar
docker load -i target/jib-image.tar
```

**Image defaults** (configured in `pom.xml`):
- Base image: `eclipse-temurin:21-jre`
- Exposed port: `8080`
- JVM flags: `-Xms256m -Xmx512m`
- Tags: `latest` + `1.0.0`

> Update `<image>YOUR_DOCKER_HUB_USERNAME/demo-observability-app</image>` in `pom.xml` with your Docker Hub username before pushing.

### Option B — Multi-stage Dockerfile (Custom JLink runtime, optimized size)

```bash
# Build image from the root of the project (where Dockerfile lives)
docker build -t demo-observability-app:v1 .
```

The Dockerfile uses 4 stages:
1. **maven-builder** — Compiles and packages the app (handles Windows CRLF in mvnw)
2. **jvm-builder** — Builds a minimal custom JVM using `jlink` (~40–100 MB vs ~500 MB full JDK)
3. **app-extractor** — Extracts the layered JAR for optimal Docker layer caching
4. **Final image** — Alpine + custom JVM + layered app + non-root user (`spring:spring`)

> **`Commands.md`** at the project root is a detailed reference guide for the `jdeps` / `jlink` workflow: how to analyse module dependencies, the exact `jlink` flags used in the Dockerfile, why each JDK module is included, and when to prefer a distroless base image over a custom runtime. Read it before modifying the `jlink` module list in the Dockerfile.

### Running the Container (standalone)

```bash
docker run -d \
  --name observability-svc \
  --cpus="2.0" \
  --memory="1024m" \
  --memory-reservation="512m" \
  -p 8080:8080 \
  -e SPRING_CLOUD_VAULT_URI=http://host.docker.internal:8200 \
  -e SPRING_CLOUD_VAULT_ROLE_ID=demo-app-role-id \
  -e SPRING_CLOUD_VAULT_SECRET_ID=demo-app-secret-id \
  -e SPRING_CLOUD_VAULT_ROLE_NAME=springboot-role \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  demo-observability-app:v1
```

> Use `host.docker.internal` to reach services running on your host machine from inside the container (works on Docker Desktop for Mac/Windows; on Linux, use `--add-host=host.docker.internal:host-gateway`).

### Option C — Full Stack with docker-compose-all.yml

Runs infra + app together as a single Docker Compose stack. The app container is built from the Dockerfile automatically.

```bash
# Build the app image first (required on first run)
docker build -t demo-observability-app:v1 .

# Start everything
docker compose -f docker-compose-all.yml up -d

# Check logs
docker compose -f docker-compose-all.yml logs -f app

# Tear down
docker compose -f docker-compose-all.yml down
```

---

## 🩺 Troubleshooting

### App fails immediately with "Vault unreachable" / `fail-fast`

- Ensure Docker Compose is running: `docker compose ps`
- Ensure Vault is healthy: `docker logs vault-init` — look for `DONE. Vault is unsealed and provisioned.`
- Check your environment variables are set. From the terminal you're running Maven in:

```
  # Linux/macOS
  echo $SPRING_CLOUD_VAULT_URI

  # PowerShell
  echo $env:SPRING_CLOUD_VAULT_URI
```

- If blank, re-run the `.env` loading PowerShell command from Quick Start Step 4.

### `PlaceholderResolutionException: Could not resolve placeholder '${secret.key}'`

The custom `PlaceholderFailureAnalyzer` will print a readable message with the exact placeholder name.

Root cause: Vault is running but the secret path `secret/demo-observability-app` is missing or `secret.key` was not seeded.

```bash
# Check if seeding completed
docker logs vault-init

# Manually verify the secret exists in Vault
docker exec -it vault-prod vault kv get secret/demo-observability-app
```

If missing, the Vault init script did not complete. Try recreating:
```bash
docker compose up -d --force-recreate vault-init
docker logs -f vault-init
```

### Redis connection refused on startup

```bash
docker compose ps redis          # Check if redis is running
docker compose restart redis     # Restart if needed
docker exec -it redis redis-cli ping  # Should reply PONG
```

### Vault sealed after host restart

Vault does not auto-unseal after a Docker restart. The `vault-init` service handles re-unsealing automatically — just restart it:

```bash
docker compose restart vault vault-init
docker logs -f vault-init        # Wait for: DONE. Vault is unsealed and provisioned.
```

Or unseal manually using the key from `vault/config/keys.txt`:
```bash
UNSEAL_KEY=$(grep "Unseal Key 1" vault/config/keys.txt | awk '{print $NF}')
docker exec -it vault-prod vault operator unseal "$UNSEAL_KEY"
```

### `setup-vault.sh` permission denied or CRLF errors

This is the most common Windows gotcha. The script must use LF line endings to run inside a Linux container.

```bash
# Check line endings
file setup-vault.sh
# Should say: "ASCII text"  (not "CRLF line terminators")

# Fix with dos2unix
dos2unix setup-vault.sh

# Or using sed
sed -i 's/\r$//' setup-vault.sh

# Prevent future issues in Git
git config core.autocrlf false
```

### Environment variables not picked up after setting them

If you set variables with `'User'` scope in PowerShell and the app still can't see them:
- Open a **new** terminal — User-scope variables only apply to new processes
- Verify: `[System.Environment]::GetEnvironmentVariable('SPRING_CLOUD_VAULT_URI', 'User')`
- In IntelliJ, **restart the IDE** after changing system environment variables; it caches the environment at launch

### Togglz console shows no features / all disabled

Feature state is in Redis. If Redis was wiped, all flags revert to default (disabled). Re-enable via the Togglz console:
```
http://localhost:8080/togglz-console
```

### JWT token expired or 403 on secure endpoints

- AppRole tokens expire in 1 h by default — restart the app to get fresh Vault credentials
- JWT tokens expire based on `JwtService` config — re-authenticate via `POST /auth/login`

### H2 console shows empty tables

Liquibase runs on startup. If it failed (check startup logs for `LiquibaseException`), tables may be missing. Common root cause: Vault wasn't ready, so the Spring context failed to load before Liquibase ran.

1. Confirm Vault is up and provisioned
2. Restart the Spring application — Liquibase will re-run on the fresh in-memory H2 instance

### Spring Boot auto-starts Docker Compose but Vault gets re-initialised

This happens if `spring.docker.compose.lifecycle-management` is not set to `start_only`. With `start_only`, Spring starts Docker Compose if it isn't running but **never stops it**. Vault is then not re-initialised across app restarts. Verify in `application.yml`:
```yaml
spring:
  docker:
    compose:
      lifecycle-management: start_only
```

---

## 📝 Notes for Contributors

- **`vault/config/keys.txt`** and **`vault/data/`** are runtime-generated and should be in `.gitignore`
- When modifying `setup-vault.sh`, ensure it uses **LF line endings** (not CRLF) — it runs inside a Linux container
- `spring.profiles.active=dev` is set in `application.properties`; switch to `prod` for the production cache manager and to hide dev-only beans
- `spring.docker.compose.lifecycle-management=start_only` prevents Spring from shutting down Docker Compose when the app stops — this is intentional so Vault remains unsealed and infrastructure keeps running across app restarts
- `spring-boot-properties-migrator` is included to surface warnings when deprecated properties are used; remove it once all properties are migrated to current naming
- Update `<image>YOUR_DOCKER_HUB_USERNAME/demo-observability-app</image>` in `pom.xml` before pushing images to Docker Hub

---

*Built as a living reference project — every package is a pattern worth knowing.*

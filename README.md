# Spring Boot — Application POC

A comprehensive **Spring Boot proof-of-concept** application that demonstrates a wide range of real-world features in one runnable project — from security and secret management to caching, feature flags, AOP, observability, and much more.

---

## 📑 Table of Contents

- [Feature Highlights](#-feature-highlights)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Infrastructure Setup (Docker)](#-infrastructure-setup-docker--do-this-first)
- [Application Setup](#-application-setup)
- [Environment Variables Reference](#-environment-variables-reference)
- [Eclipse IDE — Special Note](#-eclipse-ide--special-note)
- [Vault — Important Notes](#-vault--important-notes)
- [Running & Exploring Features](#-running--exploring-features)
- [API Reference](#-api-reference)
- [Observability Endpoints](#-observability-endpoints)

---

## ✨ Feature Highlights

### 🔐 Security
- **JWT Authentication** — Stateless token-based auth via `JwtAuthFilter` and `JwtService`
- **Role-Based Access Control** — `ADMIN` and `USER` roles enforced with `@PreAuthorize`
- **DB-Backed UserDetails** — `DatabaseUserDetailsService` loads users from H2 via JPA
- **BCrypt Password Encoding** — Passwords hashed with `BCryptPasswordEncoder`
- **Session Management** — Explicitly stateless; session invalidation endpoints provided
- **CORS Support** — `@CrossOrigin` configured for localhost React clients

### 🔑 Secret Management — HashiCorp Vault
- **AppRole Authentication** — Spring Cloud Vault connects via `role-id` / `secret-id`
- **KV-V2 Secrets Engine** — Application secrets stored at `secret/demo-observability-app`
- **Auto-inject Secrets** — `${secret.key}` injected directly from Vault at startup
- **Runtime Secret Management** — Read, list, and write secrets via `VaultController`
- **Automated Vault Bootstrap** — `setup-vault.sh` initialises, unseals, configures AppRole policy, and seeds data automatically via the `vault-init` Docker service

### 🗄️ Caching
- **Redis Cache (dev profile)** — Full Redis-backed cache via Spring Cache abstraction
- **Profile-aware CacheManager** — `dev` → Redis, `prod` → ConcurrentMapCache
- **Cache Warm-up** — `CacheWarmup` pre-populates cache on startup
- **Togglz Feature State in Redis** — Feature flag state persisted in Redis

### 🚩 Feature Flags — Togglz
- Three runtime-toggleable flags: `IS_FEATURE1_ENABLED`, `IS_FEATURE2_ENABLED`, `IS_FEATURE3_ENABLED`
- **Togglz Admin Console** at `/togglz-console` to flip flags without redeployment
- Feature state persisted in Redis (`togglz-` key prefix)

### ⚙️ Spring AOP
- **`@Around` Advice** — Intercepts `DemoAspectService.serviceMethod()` and appends `[Modified by Aspect]` to the return value
- **`@Before` Advice** — Captures method arguments before execution and logs them conditionally

### ⏱️ Scheduled Tasks
- **`fixedDelay` task** — Next execution starts 5 s *after* the previous one finishes (demonstrating sequential behaviour)
- **`fixedRate` task** — Fires every 5 s *regardless* of previous completion (demonstrating potential overlap)
- Thread-pool size set to `3` to allow concurrent execution without blocking

### 🔄 Async Operations
- `@EnableAsync` with a **default Spring thread pool** (`asyncOperation`)
- **Custom `ThreadPoolTaskExecutor`** (`transcodingPoolTaskExecutor`) with configurable core/max pool size — used by `asyncCustomOperation`
- Both exposed as REST endpoints for direct comparison

### 🌐 Spring MVC — Comprehensive Annotation Coverage
| Annotation | Demonstrated In |
|---|---|
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

### 🫘 Bean Lifecycle & DI Patterns
- **Singleton vs. Prototype scope** — `Scope1` compared live via `ApplicationContext.getBean()`
- **`@Qualifier`** — Disambiguating multiple `MultiAutowiredBean` and `First` candidates
- **Constructor injection** (preferred) vs. field injection (shown for contrast)
- **`@Lazy`** — Used in `SecurityConfig` to break a circular dependency with `JwtAuthFilter`

### 🔀 Conditional Bean Creation
- `@ConditionalOnProperty` — Two `ConditionalFirst` beans registered under different `demo.first.enabled` values
- `@ConditionalOnClass` — Bean registered only when Micrometer is on the classpath
- `@Profile("dev")` / `@Profile("prod")` — Profile-specific beans and cache managers

### 📡 Spring Events
- `UserCreatedEvent` published via `ApplicationEventPublisher` after successful user creation
- `UserEventListener` handles the event asynchronously

### ✅ Validation
- **Bean Validation** — `@Valid` on `UserDto` with `BindingResult` for fine-grained error maps
- **Custom Annotation `@TrimmedLength`** — Validates string length after trimming whitespace
- **Custom Annotation `@DynamicMin`** — Min value resolved dynamically from properties
- **Custom `ValidationMessages.properties`** — Externalised validation error messages
- **Global Exception Handler** — `@RestControllerAdvice` in `GlobalExceptionHandler`; random exception thrown from `createUserFromForm` to demonstrate it

### 📦 Persistence
- **H2 In-Memory Database** — Zero-config dev database; console at `/h2-console`
- **Spring Data JPA** — Repositories for `User`, `Product`, `Category`
- **Liquibase Migrations** — Schema and seed data managed via `db/changelog/db.changelog-master.xml`
- **`open-in-view=false`** — Explicitly disabled to avoid lazy-loading pitfalls

### 🔗 HATEOAS
- `ProductController` returns `EntityModel<Product>` and `PagedModel<EntityModel<Product>>`
- `ProductModelAssembler` adds hypermedia links
- Paginated product search and category filtering

### 📊 Observability
- **Spring Actuator** — `health`, `info`, `metrics`, `prometheus` endpoints
- **Micrometer + Prometheus** — `/actuator/prometheus` for scraping
- **Custom Metrics** — `demo.custom.requests` counter registered via `MeterRegistry`

### 🛠️ Other Notable Patterns
- **`@ConfigurationProperties`** — `UserProperties` maps `user.min.role.length` / `user.max.role.length`
- **SpEL (Spring Expression Language)** — `@Value("#{T(java.lang.Math).random() * 100}")` for runtime random injection
- **Thymeleaf** — Multi-step user registration form (`user-step1.html` → `user-step2.html` → `user-complete.html`)
- **`@Value` with defaults** — `${demo.name:Default Name}` pattern
- **Swagger / OpenAPI** — UI at `/swagger-ui.html`, spec at `/v3/api-docs`

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.x |
| Security | Spring Security, JWT |
| Secret Management | HashiCorp Vault (KV-V2, AppRole) |
| Caching | Spring Cache + Redis |
| Feature Flags | Togglz |
| Database | H2 (in-memory) |
| Migrations | Liquibase |
| Async / Scheduling | Spring `@Async`, `@Scheduled` |
| Observability | Spring Actuator, Micrometer, Prometheus |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| AOP | Spring AOP (AspectJ proxy) |
| Templates | Thymeleaf |
| Infrastructure | Docker Compose (Redis, HashiCorp Vault) |

---

## 📁 Project Structure

```
project/
├── docker-compose.yml              # Infrastructure: Redis + Vault + vault-init
├── setup-vault.sh                  # Vault bootstrap script (init, unseal, AppRole, seed)
├── vault/
│   ├── config/                     # Vault HCL config + generated keys.txt (gitignore!)
│   └── data/                       # Vault file storage (keep outside project — see notes)
└── src/main/java/com/applicationPOC/
    ├── aspects/                    # AOP — @Around, @Before
    ├── config/                     # SecurityConfig, AsyncConfig, CacheConfig, ConditionalConfig, ...
    ├── controller/                 # PublicController, SecureController, AuthController,
    │                               #   ProductController, VaultController, BinderController, ...
    ├── customAnnotation/           # @TrimmedLength, @DynamicMin + validators
    ├── domain/                     # Product, Category, ProductModelAssembler
    ├── event / eventListeners/     # UserCreatedEvent + UserEventListener
    ├── metrics/                    # CustomMetricsConfig (Micrometer counter)
    ├── model/                      # User, UserDto, Scope1, First, Second, ...
    ├── repository/                 # JPA repositories
    ├── scheduledJobs/              # ScheduledPrint (fixedDelay vs fixedRate), DynamicScheduler
    ├── security/                   # JwtAuthFilter, JwtService, DatabaseUserDetailsService, ...
    ├── service/                    # DemoService, ProductService, UserService, FeatureService
    └── togglzFeature/              # Features enum + TogglzConfigurations
```

---

## 🐳 Infrastructure Setup (Docker) — Do This First

> **Why separate?** Infrastructure (Redis, Vault) can keep running across application restarts and redeployments. Restarting the infra on every app change is wasteful and, in the case of Vault, requires re-unsealing.

### Step 1 — Start the infrastructure

```bash
docker compose up -d
```

This spins up three services:

| Service | Image | Port | Purpose |
|---|---|---|---|
| `redis` | `redis:8.4.2-alpine3.22` | `6379` | Cache + Togglz state store |
| `vault-prod` | `hashicorp/vault:1.15` | `8200` | Secret management |
| `vault-init` | `hashicorp/vault:1.15` | — | One-shot bootstrap (init, unseal, AppRole, seed) |

### Step 2 — Retrieve Vault credentials from logs

Once `vault-init` completes, it prints the `role-id` and `secret-id` to its logs:

```bash
docker logs vault-init
```

Look for the **Role ID** and **Secret ID** lines — you will need these as environment variables when starting the Spring Boot application.

> **Note:** The `role-id` is deterministically set to `demo-app-role-id` and `secret-id` to `demo-app-secret-id` in `setup-vault.sh`. If you change them there, update your environment variables accordingly.

### Vault data file location

The Vault container maps its data to `./vault/data` **inside the project folder**. This is fine for exploration, but for a stable setup you should **map the volume to a path outside the project directory** so that Vault data survives project cleans and is independent of application changes.

```yaml
# docker-compose.yml — recommended change for production-like setups
volumes:
  - /some/path/outside/project/vault/data:/vault/file
```

### Recreating the Vault container

If you need to start fresh with a new Vault container:

1. Delete `./vault/config/keys.txt`
2. Delete the contents of `./vault/data/`
3. Run `docker compose up -d --force-recreate vault vault-init`

> If you skip steps 1 & 2, the new container will inherit the old sealed state and fail to initialise correctly.

---

## ⚙️ Application Setup

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Step 1 — Set environment variables

The application requires these environment variables at startup:

| Variable | Description | Example |
|---|---|---|
| `VAULT_URI` | Vault server URL | `http://localhost:8200` |
| `VAULT_ROLE_ID` | AppRole Role ID (from vault-init logs) | `demo-app-role-id` |
| `VAULT_SECRET_ID` | AppRole Secret ID (from vault-init logs) | `demo-app-secret-id` |
| `VAULT_ROLE_NAME` | AppRole name (default: `springboot-role`) | `springboot-role` |

### Step 2 — Build and run

```bash
# Build
mvn clean package -DskipTests

# Run with environment variables
VAULT_URI=http://localhost:8200 \
VAULT_ROLE_ID=demo-app-role-id \
VAULT_SECRET_ID=demo-app-secret-id \
mvn spring-boot:run
```

Or via the JAR:

```bash
VAULT_URI=http://localhost:8200 \
VAULT_ROLE_ID=demo-app-role-id \
VAULT_SECRET_ID=demo-app-secret-id \
java -jar target/demo-observability-app-*.jar
```

The application starts on **`http://localhost:8080`**.

---

## 🌐 Environment Variables Reference

```properties
# Vault connection
VAULT_URI=http://localhost:8200
VAULT_ROLE_ID=demo-app-role-id
VAULT_SECRET_ID=demo-app-secret-id
VAULT_ROLE_NAME=springboot-role     # optional — default is springboot-role

# Other values (match your local setup if changed)
# spring.data.redis.host — default: localhost
# spring.data.redis.port — default: 6379
# server.port             — default: 8080
```

---

## 🖥️ Eclipse IDE — Special Note

Eclipse does **not** natively support `.env` files for loading environment variables into run configurations.

**Option A — Eclipse Plugin**
Install a plugin such as *EnvFile* or *Spring Tools 4* that adds `.env` file support to the Run Configuration dialog.

**Option B — Manual Runtime Environment Variables**
1. Open **Run → Run Configurations…**
2. Select your Spring Boot application
3. Go to the **Environment** tab
4. Add each variable (`VAULT_URI`, `VAULT_ROLE_ID`, `VAULT_SECRET_ID`, `VAULT_ROLE_NAME`) manually with the values obtained from the `vault-init` container logs

---

## 🔐 Vault — Important Notes

| Concern | Recommendation |
|---|---|
| **keys.txt** | Generated by `vault-init` inside `./vault/config/`. Add to `.gitignore`. Keep outside project for production. |
| **vault/data/** | Vault's file storage backend. Keep outside the project folder so Vault state is independent of code changes. |
| **Recreating Vault** | Always delete `keys.txt` **and** `vault/data/` before recreating the container; otherwise the new container inherits the previous (sealed) state. |
| **Startup order** | Always start Docker Compose **before** the Spring Boot app. The app fails fast (`fail-fast: true`) if Vault is unreachable. |
| **TTL** | AppRole tokens expire in 1 h (max 4 h). For long dev sessions, ensure Vault is still running and the app can re-authenticate. |

---

## 🚀 Running & Exploring Features

### Swagger UI (Recommended)

```
http://localhost:8080/swagger-ui.html
```

All REST endpoints are documented here. Use it to try each feature interactively.

### H2 Database Console

```
http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:demo
# Username: sa  |  Password: (blank)
```

### Togglz Admin Console

```
http://localhost:8080/togglz-console
```

Toggle `IS_FEATURE1_ENABLED`, `IS_FEATURE2_ENABLED`, `IS_FEATURE3_ENABLED` on/off at runtime without restarting the application. State is persisted in Redis.

### Vault UI

```
http://localhost:8200/ui
# Token: see vault-init logs (Initial Root Token)
```

### Getting a JWT Token

```bash
# 1. First, create a user (or use one seeded by Liquibase)
POST http://localhost:8080/api/public/users
Content-Type: application/json
{
  "name": "John",
  "email": "john@example.com",
  "password": "password123",
  "role": "USER"
}

# 2. Generate a BCrypt hash for storage (helper endpoint)
GET http://localhost:8080/api/public/hash/{yourPassword}

# 3. Login to get a JWT
POST http://localhost:8080/auth/login
Content-Type: application/json
{
  "username": "john@example.com",
  "password": "password123"
}
# Response: { "token": "eyJ..." }

# 4. Use the token in subsequent requests
GET http://localhost:8080/api/secure/me
Authorization: Bearer eyJ...
```

---

## 📡 API Reference

### Public Endpoints (`/api/public/**`) — No auth required

| Method | Path | Feature Demonstrated |
|---|---|---|
| `GET` | `/api/public/ping` | Health check |
| `GET` | `/api/public/cached/{id}` | `@Cacheable` + `@PathVariable` |
| `GET` | `/api/public/search?keyword=&page=` | `@RequestParam` |
| `GET` | `/api/public/user-agent` | `@RequestHeader` |
| `GET` | `/api/public/welcome` | `@CookieValue` + cookie set |
| `GET` | `/api/public/greet` | `@RequestAttribute` (set by `UsernameFilter`) |
| `POST` | `/api/public/users` | `@RequestBody` + Bean Validation + events |
| `GET` | `/api/public/users/{id}` | JPA lookup |
| `POST` | `/api/public/users/form` | `@ModelAttribute` + Global Exception Handler |
| `GET` | `/api/public/async/{input}` | `@Async` (default pool) |
| `GET` | `/api/public/custom-async/{input}` | `@Async` (custom pool) |
| `GET` | `/api/public/message` | Prototype scope + SpEL |
| `GET` | `/api/public/scope` | Singleton scope |
| `GET` | `/api/public/aspect-value/{name}` | AOP `@Around` (return modification) |
| `GET` | `/api/public/hash/{password}` | BCrypt hash generator |
| `GET` | `/api/public/conditional-first` | `@ConditionalOnProperty` |
| `GET` | `/api/public/user-properties` | `@ConfigurationProperties` |
| `GET` | `/api/public/feature/{feature}` | Togglz feature flag check |
| `GET` | `/api/public/name` | `@Value` injection with default |

### Auth Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/login` | Authenticate and receive a JWT token |

### Secure Endpoints (`/api/secure/**`) — JWT required

| Method | Path | Role Required |
|---|---|---|
| `GET` | `/api/secure/me` | Any authenticated user |
| `GET` | `/api/secure/common` | Any authenticated user |
| `GET` | `/api/secure/user` | `ROLE_USER` |
| `GET` | `/api/secure/admin` | `ROLE_ADMIN` |
| `GET` | `/api/secure/logout` | Any authenticated user |
| `GET` | `/api/secure/logout-force` | Any authenticated user |

### Product Endpoints (`/api/products/**`) — JWT required

| Method | Path | Feature |
|---|---|---|
| `GET` | `/api/products/{id}` | HATEOAS `EntityModel` |
| `GET` | `/api/products/by-category?category=` | Paginated + HATEOAS |
| `GET` | `/api/products/search?q=` | JPA search with pagination |
| `GET` | `/api/products/expensive?minPrice=` | Custom JPA query |
| `POST` | `/api/products` | Create product |
| `PATCH` | `/api/products/{id}/stock` | Partial update |

### Vault Endpoints (`/vault/**`)

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/vault/secret` | None | Read `secret.key` injected from Vault |
| `GET` | `/vault/secrets` | None | List all secrets for the app |
| `GET` | `/vault/secretByKey?key=` | None | Read a specific secret key |
| `POST` | `/vault/manage/secrets?key=&value=` | `ROLE_ADMIN` | Write / update a secret |

### Binder Endpoint (`/api/binder/**`) — JWT + ROLE_USER

| Method | Path | Feature |
|---|---|---|
| `POST` | `/api/binder/register` | `@InitBinder` + `StringTrimmerEditor` |

---

## 📊 Observability Endpoints

| Endpoint | Access | Description |
|---|---|---|
| `/actuator/health` | Public | App + dependency health |
| `/actuator/info` | Public | App name & version |
| `/actuator/metrics` | `ROLE_ADMIN` | All registered metrics |
| `/actuator/prometheus` | `ROLE_ADMIN` | Prometheus scrape endpoint |
| `/swagger-ui.html` | Public | Interactive API docs |
| `/v3/api-docs` | Public | OpenAPI JSON spec |
| `/h2-console` | Public | H2 in-memory DB console |
| `/togglz-console` | Public (dev) | Feature flag management |

---

## 📝 Notes for Contributors

- The `vault/config/keys.txt` and `vault/data/` directory are generated at runtime and should be added to `.gitignore` — they are included in this repository **for reference only**
- When you modify `setup-vault.sh`, ensure it uses **LF line endings** (not CRLF), as it runs inside a Linux container
- The `spring.profiles.active=dev` is set in `application.properties`; switch to `prod` for the production cache manager
- `spring.docker.compose.lifecycle-management=start_only` prevents Spring from shutting down Docker Compose when the app stops — intentional, so infrastructure keeps running

---

*Built as a living reference project — every package is a pattern worth knowing.*

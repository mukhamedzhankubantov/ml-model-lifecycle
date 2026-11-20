# ML Model Lifecycle

Spring Boot integration for ML Model Lifecycle management using Clean Architecture principles.

## Package Architecture

```
  dto          client        handler        config
  (JSON later) (HTTP later)  (HTTP week 9)   Application
                                             @Service
        \            \            /                |
         \            \          /            injects Rule
          \            \        /
                        domain
               *Id  *Status  *Policy
               Rule  + two implementations
                     (no Spring)
```

## Features & Domain Rules
- **ModelId**: Immutable domain identifier with validation.
- **ModelStatus**: Enum defining lifecycle states (`DRAFT`, `EVALUATED`, `PRODUCTION`, `REJECTED`).
- **ModelPolicy**: Clean Java rules engine managing status transitions.
    - **TransitionRule**: Prevents skipping directly from `DRAFT` to `PRODUCTION`.
    - **AccuracyStopRule**: Enforces minimum accuracy threshold (80%) before moving to `PRODUCTION`.

## Tasks Covered
- `MLOPS-1`: Setup Spring Boot application structure
- `MLOPS-2`: Domain Core implementation (`ModelId`, `ModelStatus`)
- `MLOPS-3`: Domain Rules & Policy (`ModelRule`, `TransitionRule`, `AccuracyStopRule`, `ModelPolicy`)
- `MLOPS-4`: Spring Boot Beans configuration & `ModelService`

## Status Table (tested)

| From | To | Accuracy | Result |
| --- | --- | --- | --- |
| DRAFT | EVALUATED | 0.90 | allowed |
| EVALUATED | PRODUCTION | 0.90 | allowed |
| DRAFT | PRODUCTION | 0.90 | IllegalStateException |
| EVALUATED | PRODUCTION | 0.50 | IllegalStateException |


## Lab 4: REST contract and platform
- `POST /api/v1/models` creates a model (201 + `Location`), `GET /api/v1/models/{id}` reads it by UUID (200).
- Errors use one body: `code`, `message`, `correlationId`. 400 `VALIDATION`, 404 `NOT_FOUND`, 409 `DUPLICATE`, 415 `UNSUPPORTED`, 500 `INTERNAL`.
- `handler` package holds the controller, request/response/error records and `ApiExceptionHandler`. `domain` has no Spring, no `ResponseEntity`, no `jakarta.validation`.
- Swagger UI: `/swagger-ui.html`, spec: `/v3/api-docs`.
- Config: `application.yaml` (shared), `application-dev.yaml` (localhost, `css`/`css`), `application-prod.yaml` (env vars `CSS_DB_URL`, `CSS_DB_USER`, `CSS_DB_PASSWORD`, `CSS_DOWNSTREAM_URL`). `app.downstream` is bound by `@ConfigurationProperties`.
- Actuator exposes only `health` and `info`.
- Run: start PostgreSQL, then `mvn -q verify` or `mvn spring-boot:run` (profile `dev` is the default).

## Lab 5: Security rules

Two roles: `DATA_SCIENTIST` (junior) and `ML_LEAD` (senior).
Locked action: `promote` (moves a model to `PRODUCTION`).

| Action | DATA_SCIENTIST | ML_LEAD | No token |
| --- | --- | --- | --- |
| `POST /api/v1/auth/login` | anyone | anyone | allowed |
| Create and read a model (`POST /api/v1/models`, `GET /api/v1/models/{id}`) | yes | yes | 401 |
| Evaluate: `POST /api/v1/models/{id}/evaluate` (DRAFT to EVALUATED) | yes | yes | 401 |
| Locked: `POST /api/v1/models/{id}/promote` | 403 | yes | 401 |
| `GET /api/me` | yes | yes | 401 |
| `GET /actuator/health` | anyone | anyone | allowed |
| Auth page (`/`, `/index.html`, `/styles.css`, `/app.js`, `/img/**`) | anyone | anyone | allowed |
| springdoc UI and `/v3/api-docs/**` | anyone | anyone | allowed |
| Anything else | 403 | 403 | 401 |

Error bodies stay `code`, `message`, `correlationId`:
- no or bad token: 401 `UNAUTHENTICATED`, `valid token required`
- wrong role: 403 `FORBIDDEN`, `not allowed for your role`
- wrong login: 401 `UNAUTHENTICATED`, `bad username or password`
- the policy forbids a move (for example DRAFT to PRODUCTION): 409 `POLICY_VIOLATION`, the row does not change

`/actuator/info`, `env`, `heapdump` and every other URL fall under "Anything else".

### Login and token

`POST /api/v1/auth/login` with `{"username","password"}` returns `accessToken`, `tokenType` (`Bearer`), `expiresIn` (`900`).
The token is HS256, issuer `ml-model-lifecycle`, lifetime 15 minutes, claims `iss`, `sub`, `iat`, `exp`, `roles` (role names without `ROLE_`).
Unknown user and wrong password give the same 401. Send the token as `Authorization: Bearer <token>`.

### Secret and users

- `app.jwt.secret` is at least 32 bytes. It is set in `application-dev.yaml` (classroom value) and `application-prod.yaml` (`${CSS_JWT_SECRET}`, no default). `application.yaml` has none, and `src/main/java` has no secret literal. A short secret fails at startup.
- Dev users exist only on the `dev` profile (`InMemoryUserDetailsManager`, BCrypt hashes, no `{noop}`):

| Username | Password | Role |
| --- | --- | --- |
| `scientist` | `scientist-dev` | `DATA_SCIENTIST` |
| `lead` | `lead-dev` | `ML_LEAD` |

### Locked action

`POST /api/v1/models/{id}/promote` with `{"accuracy": 0.91}`. The actor is `Authentication.getName()`; an `actor` or `role` field in the body is ignored.
The URL rule and `@PreAuthorize` on `ModelPromotionService` name the same role (`ML_LEAD`). A model must be `EVALUATED` with accuracy of at least 0.80.
`POST /api/v1/models/{id}/evaluate` moves a `DRAFT` model to `EVALUATED` (both roles), so the page can reach a promotable model.

### Auth page

Run `mvn spring-boot:run` (profile `dev`) and open `http://localhost:8080/`. The page is served from `src/main/resources/static/`
(`index.html`, `styles.css`, `app.js`, `img/`), has no external libraries, and keeps the token in a JavaScript variable only.
Sign in as `scientist` and run Promote to see 403 and an unchanged status; log out, sign in as `lead` and promote to see `PRODUCTION`;
log out and any call is 401. Swagger UI: `/swagger-ui.html`.
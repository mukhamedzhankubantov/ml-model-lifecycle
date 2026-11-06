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
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
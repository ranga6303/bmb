# Backend Codebase Overview (`demo`)

This document gives a practical map of the backend project in `demo/`.

## What This Service Does

Spring Boot API for a college attendance platform with:

- Role-based authentication and authorization
- JWT access + refresh token session management
- Teacher-managed attendance sessions
- Student attendance marking with device/signature checks
- HOD reporting and assignment workflows
- Admin role/device-management operations

## Tech Stack

- Java 17
- Spring Boot 4.0.3
- Spring Web, Spring Security, Spring Data JPA, Validation, Mail
- MySQL (runtime), H2 (tests)
- JWT (`jjwt` 0.12.7)
- Maven wrapper (`mvnw`, `mvnw.cmd`)

## Project Structure

Top-level backend code lives in `src/main/java/com/example/demo/`:

- `auth/`: auth and greeting controllers
- `controller/`: student, teacher, session, HOD, admin, report endpoints
- `service/`: business logic and workflow orchestration
- `repository/`: Spring Data repositories
- `entity/`: JPA entities and enums
- `dto/`: request/response objects
- `security/`: JWT utility, bearer filter, user details service
- `config/`: security config, email config, bootstrap, schedulers
- `exception/`: centralized exception handling

## Runtime Entry Points

- Main app: `src/main/java/com/example/demo/DemoApplication.java`
- Deployment command target: `Procfile` (`java -jar target/demo-0.0.1-SNAPSHOT.jar`)

Startup loads `.env` values via `dotenv-java` when system env vars are missing.

## Local Commands

From `demo/`:

- Run API: `.\mvnw spring-boot:run`
- Run tests: `.\mvnw test`
- Build jar: `.\mvnw clean package`
- Run jar: `java -jar target/demo-0.0.1-SNAPSHOT.jar`

## Core Request Flow

1. Request enters Spring Security filter chain.
2. `JwtAuthenticationFilter` validates bearer token and session/device constraints.
3. Controller validates/authorizes request.
4. Service executes business logic.
5. Repository persists/queries entities.
6. `GlobalExceptionHandler` normalizes error responses.

## Main Domain Areas

- **Auth**: registration OTP, login, refresh, logout, password reset
- **Sessions**: create, lock, approve, cancel attendance sessions
- **Attendance**: auto mark (student), manual mark (teacher), buffer-to-finalize flow
- **Reports**: student report, section report, department analytics
- **Governance**: HOD section/class-teacher assignments, admin role/device actions

## Security Model Snapshot

- Stateless JWT auth with method-level authorization (`@PreAuthorize`)
- Refresh sessions persisted and revocable (`UserSession`)
- Optional device binding enforced in JWT/session validation
- Role + permission authorities derived from `Role` enum

## Data and Scheduling

- Relational model includes users, student/teacher profiles, sessions, attendance, tokens, device-change requests, and audit logs.
- Scheduled tasks auto-progress stale sessions:
  - active -> locked
  - old locked -> cancelled (with buffer cleanup)

## Config and Environment

`application.properties` expects env-driven configuration for:

- server/JWT settings
- datasource/JPA settings
- mail settings
- app URL and bootstrap admin inputs

## Important Notes

- This backend is designed to pair with a mobile client.
- In this workspace, the React Native client is located in sibling folder `f1/`.
- Use `docs/api_reference.md` for endpoints and `docs/system_flow.md` for sequence-level behavior.

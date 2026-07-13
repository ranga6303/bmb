# Project Overview

This project is a Spring Boot backend for a college attendance management system. It supports account registration, JWT login, role-based APIs, attendance sessions, BLE beacon checks, ECDSA device signatures, device change requests, and attendance reporting for students, teachers, HODs, and admins.

## Tech Stack

| Area | Verified implementation |
|---|---|
| Language | Java 17 (`pom.xml`) |
| Framework | Spring Boot 4.0.3 |
| Build | Maven wrapper (`mvnw`, `mvnw.cmd`) |
| Web | `spring-boot-starter-web` |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL by default, H2 for tests |
| Security | Spring Security, method security, JWT, BCrypt |
| JWT library | `io.jsonwebtoken:jjwt-*` 0.12.7 |
| Email | Resend Java SDK through `SmtpEmailService` |
| Env loading | `dotenv-java` loads `.env` into system properties in `DemoApplication` |
| Tests | JUnit/Spring Boot tests under `src/test/java` |

## Package Structure

| Package | Purpose |
|---|---|
| `com.example.demo` | Application entry point: `DemoApplication` enables scheduling and loads `.env`. |
| `auth` | Authentication REST endpoints and a simple `/greet` endpoint. |
| `controller` | Role and feature controllers: admin, HOD, teacher, student, sessions, reports, health, test password endpoint. |
| `service` | Business logic for auth, sessions, rooms, device verification, current user lookup, email, and device changes. |
| `repository` | Spring Data JPA repositories. |
| `entity` | JPA entities and enums for users, roles, sessions, attendance, rooms, tokens, and audit logs. |
| `dto` | Request/response DTOs used by controllers and services. |
| `security` | JWT utilities, filter, user-details service, token hashing. |
| `config` | Spring Security, CORS, email beans, admin bootstrap, scheduled session expiry. |
| `exception` | Custom exceptions and global REST exception handler. |

## Important Runtime Files

| File | Purpose |
|---|---|
| `src/main/resources/application.properties` | Main runtime config with env placeholders. |
| `src/test/resources/application-test.properties` | H2/test profile config. |
| `.env` | Local secrets and environment values. Present in the workspace but ignored by `.gitignore`. |
| `Procfile` | Railway-style launch command: `java -jar target/demo-0.0.1-SNAPSHOT.jar`. |
| `seed-data.ps1` | Seed script for subjects, sections, rooms, users, and mappings. Some API paths in it no longer match the current controllers. |

## Application Entry Point

`DemoApplication` is annotated with `@SpringBootApplication` and `@EnableScheduling`. Before starting Spring, it loads `.env` using `Dotenv.configure().ignoreIfMissing().load()` and sets each entry as a system property only when the same key is not already present in the OS environment.

## Scheduled Jobs

`SessionExpiryScheduler` runs two scheduled jobs:

| Method | Schedule | Behavior |
|---|---:|---|
| `expireSessions()` | every 60 seconds | Calls `SessionService.expireActiveSessions()` to move expired `ACTIVE` sessions to `LOCKED`. |
| `autoCancelLockedSessions()` | every 300 seconds | Calls `SessionService.autoCancelLockedSessions()` to cancel `LOCKED` sessions older than one hour. |


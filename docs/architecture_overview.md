# Architecture Overview

## System Purpose

College attendance platform backend API. Teachers create timed attendance sessions tied to classrooms (BLE beacons). Students mark attendance from a bound mobile device with cryptographic proof. HODs manage teacher assignments and view department analytics. Admins govern roles, device bindings, and master-data mappings.

**Mobile client:** React Native / Expo app in sibling folder `main\f1` (not part of this repo).

---

## High-Level Architecture

```text
┌─────────────────┐     HTTPS/REST      ┌──────────────────────────────────┐
│  Mobile Client  │ ──────────────────► │  Spring Boot API (demo)          │
│  (f1/)          │   Bearer JWT        │  Port 8080                       │
└─────────────────┘                     └──────────────┬───────────────────┘
                                                       │
                       ┌───────────────────────────────┼───────────────────────┐
                       │                               │                       │
                       ▼                               ▼                       ▼
              ┌────────────────┐            ┌──────────────────┐    ┌─────────────────┐
              │  MySQL         │            │  Resend Email    │    │  Scheduled Jobs │
              │  (JPA/Hibernate)│            │  (OTP delivery)  │    │  Session expiry │
              └────────────────┘            └──────────────────┘    └─────────────────┘
```

---

## Layer Structure

```text
HTTP Request
  → SecurityConfig (CORS, CSRF off, route rules)
  → JwtAuthenticationFilter (JWT + session + device validation)
  → @RestController (validation, @PreAuthorize)
  → @Service (business logic, @Transactional)
  → @Repository (Spring Data JPA)
  → MySQL
```

### Package Map

| Package | Responsibility |
|---------|----------------|
| `auth/` | Public authentication endpoints |
| `controller/` | Role-scoped REST APIs |
| `service/` | Business workflows |
| `repository/` | Data access (16 repositories) |
| `entity/` | JPA domain model + enums |
| `dto/` | Request/response objects |
| `security/` | JWT, token hashing, user details, bearer filter |
| `config/` | Security, email, admin bootstrap, schedulers |
| `exception/` | Global error handling |

---

## Entry Points

| Entry | Location | Purpose |
|-------|----------|---------|
| Application main | `DemoApplication.java` | Loads `.env` via dotenv-java, starts Spring Boot |
| Dev server | `.\mvnw spring-boot:run` | Local development |
| Production | `Procfile` → `java -jar target/demo-0.0.1-SNAPSHOT.jar` | Heroku-style deploy |
| Data seeding | `seed-data.ps1` | HTTP-based local seeding (partially stale) |

---

## Trust Boundaries

| Boundary | Public | Authenticated |
|----------|--------|---------------|
| Routes | `/auth/**`, `/health`, `/` | All other endpoints |
| Auth mechanism | None | `Authorization: Bearer <JWT>` |
| Authorization | N/A | `@PreAuthorize` on permissions/roles |
| Session state | Stateless JWT | `UserSession` revocation checked per request |

---

## Major Subsystems

### 1. Authentication (`AuthService`, `AuthController`)
- OTP registration for preloaded student/teacher profiles
- Login with account lockout (5 failures → 15 min lock)
- JWT access + refresh token with rotation
- Password reset via email OTP

### 2. Session Lifecycle (`SessionService`, `SessionController`)
- Create → Active → Locked → Approved/Cancelled
- Auto-lock on expiry (scheduler, 60s interval)
- Auto-cancel locked sessions after 1 hour (scheduler, 300s interval)

### 3. Attendance (`SessionService`, `StudentController`)
- Student auto-mark → `AttendanceBuffer`
- Teacher manual-mark → `AttendanceBuffer`
- Approval → final `Attendance` rows (present/absent)

### 4. Device Management (`DeviceVerificationService`, `DeviceChangeService`)
- First-mark device + EC public key registration
- ECDSA signature verification (`SHA256withECDSA`)
- Admin-approved device change workflow

### 5. Reporting (`SessionService`, `HodController`, `DepartmentController`, `SectionController`)
- Student own attendance
- Section/subject attendance
- Department analytics

### 6. Governance (`AdminController`, `HodController`)
- Role assignment, device reset
- Teacher-section-subject assignments
- Class teacher assignment

---

## External Integrations

| Integration | Usage | Config |
|-------------|-------|--------|
| MySQL | Primary datastore | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Resend | Registration/password OTP email | `resend.api.key`, `app.mail.from` |
| dotenv-java | Local `.env` loading | `.env` file (gitignored) |

---

## Scheduling

`SessionExpiryScheduler`:
- Every 60s: `ACTIVE` sessions past `expiryTime` → `LOCKED`
- Every 300s: `LOCKED` sessions older than 1 hour → `CANCELLED` (buffer deleted)

---

## Scalability Considerations

**Current design suits single-institution deployment:**
- No caching layer
- Synchronous email sending
- Report endpoints perform nested loops with per-section DB queries
- `ddl-auto=update` not suitable for multi-instance schema management

**Horizontal scaling blockers:**
- Scheduler runs on every instance (duplicate session state transitions unless externalized)
- No distributed lock for session code generation (low collision risk with 6-digit codes)

---

## Related Documentation

- [Authentication & Authorization](authentication_authorization.md)
- [Database Design](database_design.md)
- [API Reference](api_reference.md)
- [Attendance Workflow](attendance_workflow.md)
- [Security Considerations](security_considerations.md)
- [Audit Report](audit_report.md)

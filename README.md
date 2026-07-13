# College Attendance Management System

A production-deployed backend for a college attendance system built with Java and Spring Boot. Supports four user roles — Student, Teacher, HOD, and Admin — each with isolated API surfaces and fine-grained permissions.

Live backend: `https://web-production-783e7.up.railway.app`

---

## What It Does

- Students mark attendance via a mobile app using BLE beacon proximity and ECDSA cryptographic signatures
- Teachers manage sessions, review live attendance buffers, and approve final records
- HODs get department-wide analytics and manage teacher assignments
- Admins handle user management, role assignment, and device change requests

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot |
| Security | Spring Security, JWT, BCrypt, ECDSA |
| Database | MySQL (production), H2 (tests) |
| ORM | JPA / Hibernate |
| Email | Resend SDK |
| Deployment | Railway |
| Mobile Client | React Native, Expo (separate repo) |

---

## Core Features

### Authentication
- JWT access tokens with refresh token rotation
- OTP-based registration and password reset via email
- BCrypt password hashing
- 5-attempt account lockout with 15-minute cooldown
- Token invalidation on password change
- Server-side session revocation on login and logout

### Role-Based Access Control
- 4 roles: Student, Teacher, HOD, Admin
- 20+ individual permissions enforced via `@PreAuthorize` annotations
- Each role has an isolated API surface

### Attendance System
- 3-minute active session window to prevent brute force
- Session codes never shown to students — fetched silently by the mobile app
- BLE beacon UUID must match the session room for physical presence verification
- ECDSA signature verification using P-256 key pairs on every attendance mark
- Students register a public key on first mark; all subsequent marks require a valid signature
- Attendance buffer for teacher review before final records are written

### Device Binding
- One device per student enforced via registered device ID
- EC public key stored per student
- Admin-approved device change request workflow
- Immediate admin device reset with full session revocation

### Reporting
- Students: subject-wise attendance percentages
- Teachers: section-level attendance reports
- HODs: department-wide analytics across all subjects

### Admin & HOD Operations
- Teacher-section-subject mapping
- Role assignment
- Device change request approval/rejection
- Audit logging for all key actions

---

## Project Structure

```
src/
├── controller/        # REST controllers per role
├── service/           # Business logic
├── repository/        # JPA repositories
├── entity/            # Database entities
├── dto/               # Request/response DTOs
├── security/          # JWT filter, user details, token utils
├── config/            # Security config, email config, admin bootstrap
└── exception/         # Global exception handler
```

---

## API Overview

| Role | Base Path |
|------|-----------|
| Auth | `/auth/**` |
| Student | `/student/**` |
| Teacher | `/sessions/**`, `/teacher/**` |
| HOD | `/hod/**`, `/departments/**` |
| Admin | `/admin/**` |

Full API reference available in [`docs/api_reference.md`](docs/api_reference.md)

---

## Running Locally

### Prerequisites
- Java 17
- Maven
- MySQL

### Setup

1. Clone the repository
```bash
git clone https://github.com/ranga6303/demo.git
cd demo
```

2. Create a MySQL database
```sql
CREATE DATABASE attendance_db;
```

3. Set environment variables
```bash
PORT=8080
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION_MS=86400000
DB_URL=jdbc:mysql://localhost:3306/attendance_db
DB_USERNAME=root
DB_PASSWORD=yourpassword
JPA_DDL_AUTO=update
RESEND_API_KEY=your-resend-api-key
MAIL_FROM=your@email.com
ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@college.edu
ADMIN_PASSWORD=Admin@1234
```

4. Run the application
```bash
./mvnw spring-boot:run
```

Backend starts at `http://localhost:8080`

---

## Database

- MySQL in production (Railway)
- Schema auto-managed via Hibernate (`ddl-auto`)
- Master data (students, teachers, sections, subjects, rooms) seeded via `seed-data.ps1`

Full schema documented in [`docs/database_design.md`](docs/database_design.md)

---

## Documentation

| Document | Description |
|----------|-------------|
| [`docs/architecture_overview.md`](docs/architecture_overview.md) | System architecture |
| [`docs/authentication_authorization.md`](docs/authentication_authorization.md) | Auth flows and JWT structure |
| [`docs/database_design.md`](docs/database_design.md) | Schema and entity relationships |
| [`docs/attendance_workflow.md`](docs/attendance_workflow.md) | Attendance marking flow |
| [`docs/device_registration_workflow.md`](docs/device_registration_workflow.md) | Device binding and change requests |
| [`docs/api_reference.md`](docs/api_reference.md) | Full API reference |
| [`docs/audit_report_2026.md`](docs/audit_report_2026.md) | Security audit report |

---

## Security Notes

- All secrets managed via environment variables — no hardcoded credentials
- ECDSA signatures prevent attendance spoofing and replay attacks
- Hidden session codes eliminate the most common fraud vector
- Full audit log maintained for all sensitive operations

---

## Mobile App

A companion React Native app built with Expo demonstrates the student and teacher flows including BLE beacon scanning, cryptographic signing, and real-time session tracking. Available in a separate repository.

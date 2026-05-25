# Attendance System API - Project Explanation

## 1. Project Overview

This project is a backend REST API for a college attendance management system. It replaces paper-based attendance with a secure, session-based workflow where teachers create sessions, students mark attendance using beacon and device verification, and final records are stored after teacher approval.

The implementation includes OTP-based teacher registration, JWT authentication with refresh tokens, attendance session lifecycle management, and device-change workflows. Analytics and data-entry modules remain incomplete, and a few endpoints are diagnostic or placeholder.

## 2. Technology Stack

- Framework: Spring Boot 4.0.3
- Language: Java 17
- Database: MySQL 8 (JPA/Hibernate)
- Security: Spring Security + JWT (jjwt 0.12.7)
- Validation: Jakarta Bean Validation
- Email: Spring Mail (SMTP via .env) with NoOp fallback
- Scheduling: Spring @Scheduled
- Environment: dotenv-java for .env loading
- Build: Maven

## 3. Architecture Overview (Layered / MVC)

Client -> Controller -> Service -> Repository -> Database -> Response

Text diagram:

Client
  | HTTP
  v
Security Filter Chain (JWT)
  | sets SecurityContext
  v
Controller (REST endpoints)
  | calls
  v
Service (business rules, validation)
  | data access
  v
Repository (Spring Data JPA)
  | persistence
  v
MySQL

JwtAuthenticationFilter validates JWTs, revocation status, and (when present) session device IDs before controllers run.

## 4. Module Breakdown

### 4.1 Controllers

- AuthController: registration, login, refresh, password reset, logout
- StudentController: profile, active session, attendance mark, device change
- TeacherController: mapped departments, sections, subjects
- SessionController: session lifecycle operations
- AdminController: role assignment, device reset, device change approvals
- HodController: teacher/section/subject assignments
- SectionController: attendance reports by section and subject
- DepartmentController: department report placeholder
- GreetingController / TestController: utility endpoints

### 4.2 Services

- AuthService: registration, login, refresh, password reset, role assignment, device reset, audit logging
- SessionService: session creation, attendance marking, lock/approve/cancel, reports, scheduled expiry
- DeviceChangeService: device change request/approve/reject
- DeviceVerificationService: ECDSA public key registration and signature checks
- RoomService: room validation and occupancy checks
- CurrentUserService: fetch authenticated user/teacher/student
- EmailService (SmtpEmailService, NoOpEmailService): outbound email

### 4.3 Repositories

Repositories follow Spring Data JPA patterns with a few custom queries:

- UserRepository, StudentRepository, TeacherRepository
- SessionRepository, AttendanceRepository, AttendanceBufferRepository
- DeviceChangeRequestRepository, UserSessionRepository
- EmailVerificationTokenRepository, PasswordResetTokenRepository
- RoomRepository, SubjectRepository, SectionRepository
- AuditLogRepository

### 4.4 Entities (Core Data Model)

- User: account, role, lockout fields, device binding, password-change timestamps
- Student, Teacher: profiles linked to User
- Section, Subject: academic structures with many-to-many mappings
- Room: physical room and beacon metadata
- Session: attendance session with lifecycle state (ACTIVE, LOCKED, APPROVED, CANCELLED)
- AttendanceBuffer: provisional marks
- Attendance: final approved attendance
- UserSession: refresh token tracking and revocation
- DeviceChangeRequest: device change workflow
- EmailVerificationToken, PasswordResetToken
- AuditLog

## 5. Request Flow Summary

### 5.1 Authentication

1) /auth/initiate-registration -> AuthService.initiateRegistration
2) Teacher-only OTP token is generated, hashed, and emailed
3) /auth/complete-registration -> AuthService.completeRegistration creates SUBJECT_TEACHER user
4) /auth/login -> AuthService.loginInternal validates credentials, applies lockout rules, revokes prior sessions, issues JWT + refresh
5) JwtAuthenticationFilter validates JWT, checks revocation, and rejects tokens issued before the last password change
6) /auth/refresh rotates refresh tokens and issues a new JWT
7) /auth/logout revokes a refresh-token session

### 5.2 Attendance Session Creation

1) /sessions (POST) -> SessionService.createSession
2) Validations: teacher mapping, subject-section mapping, time window (07:00-21:00), no Sunday, no duplicate for day, no active/locked session for teacher/section, room validity and occupancy
3) Session stored as ACTIVE with 6-digit code and 10-minute expiry

### 5.3 Student Attendance Marking

1) /student/attendance -> SessionService.markAttendance
2) Validate active session, expiry, section membership, and beacon UUID
3) Register public key on first attendance; otherwise verify ECDSA signature
4) Enforce User.registeredDeviceId match when already set
5) Store AttendanceBuffer record

### 5.4 Session Approval

1) /sessions/{id}/lock -> SessionService.lockSession (ACTIVE -> LOCKED)
2) /sessions/{id}/approve -> SessionService.approveSession converts buffer to final Attendance, marks absences
3) /sessions/{id}/cancel -> SessionService.cancelSession deletes buffer and marks session CANCELLED

## 6. Current Progress Status

### Completed

- Teacher-centric registration with OTP emails
- JWT auth with refresh-token rotation and session revocation
- Login lockout after repeated failures
- Student attendance marking with beacon validation and ECDSA verification
- Session lifecycle: create, lock, approve, cancel
- Device change request workflow
- HOD assignment of sections and subjects
- Section-level attendance report
- Admin bootstrap for default ADMIN account
- Role assignment and device reset endpoints

### In Progress

- Department analytics report (endpoint exists but returns empty data)
- Audit trail (partial; only login success, role assignment, and device reset are logged)
- Device binding for JWT sessions (deviceId is not persisted during login)

### Not Implemented

- Data-enterer workflows to preload/manage students, teachers, rooms, subjects, sections
- Full department analytics logic
- Admin CRUD APIs for academic master data

## 7. Issues, Gaps, and TODOs

1) Login ignores deviceId; UserSession.deviceId stays null, so JWT device binding is not enforced.

## 8. Key Observations

- The system follows a layered MVC pattern with clear separation of concerns.
- Validation is enforced at both DTO and service levels for critical flows.
- Device verification is strong at attendance time, but session-level device binding is not wired in login.

## 9. References

- Root project explanation: ../PROJECT_EXPLANATION.md
- SRS: ../SRS.md

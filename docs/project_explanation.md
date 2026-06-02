# Attendance System API - Project Explanation

This document reflects the current backend source under `src/main/java/com/example/demo`, plus the project config/test files that are present in this repository.

## 1. Overview

The project is a Spring Boot backend for a college attendance system. The active code provides account registration for preloaded student and teacher profiles, login with JWT and refresh-token sessions, permission-protected attendance workflows, teacher session lifecycle operations, student attendance marking with beacon and ECDSA signature checks, report endpoints, HOD assignment workflows, admin role/device operations, and scheduled session state changes.

This document covers only the backend service in `demo/`. In this workspace, a React Native client exists separately in sibling folder `f1/`.

## 2. Tech Stack

- Java 17
- Spring Boot 4.0.3
- Maven build
- Spring Web for REST controllers
- Spring Security with stateless JWT and method-level authorization
- Spring Data JPA / Hibernate
- MySQL runtime driver
- H2 database for tests
- jjwt 0.12.7
- Jakarta Bean Validation
- Spring Mail
- Spring Scheduling
- dotenv-java dependency

`src/main/resources/application.properties` reads runtime values from environment placeholders for server port, JWT secret/expiry, database, JPA, mail, app URL, and admin bootstrap credentials.

## 3. Architecture

The code follows a controller-service-repository structure:

```text
HTTP request
  -> SecurityConfig / JwtAuthenticationFilter
  -> Controller
  -> Service
  -> Repository
  -> JPA entity/database
```

Main packages:

- `auth`: authentication and greeting controllers.
- `controller`: admin, HOD, teacher, student, session, report, health, and diagnostic routes.
- `service`: business logic for auth, attendance sessions, room validation, current-user lookup, device verification, device-change requests, and email sending.
- `repository`: Spring Data repositories and JPQL queries.
- `entity`: JPA entities and enums.
- `dto`: request and response objects.
- `security`: JWT generation/validation, token hashing, Spring `UserDetails`, and bearer-token filtering.
- `config`: security, email bean selection, admin bootstrap, and scheduled jobs.
- `exception`: global exception response handling.

## 4. Security Model

`SecurityConfig`:

- Enables method security.
- Disables CSRF.
- Uses stateless sessions.
- Allows CORS from any origin with methods `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS`.
- Permits `/auth/**`, `/`, and `/health`.
- Requires authentication for every other route.
- Installs `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

`JwtAuthenticationFilter`:

- Reads `Authorization: Bearer <token>`.
- Extracts username from JWT.
- Loads user details from `CustomUserDetailsService`.
- Validates token subject, expiry, and issue time relative to `lastPasswordChange`.
- Extracts `sessionId`.
- Requires the corresponding `UserSession` to exist and not be revoked.
- If the stored session has a device ID, requires it to match the JWT `deviceId` claim.
- Populates the Spring Security context with the user's role and permission authorities.

`CustomUserDetailsService` grants:

- `ROLE_<role name>`
- Every permission returned by `Role.getPermissions()`

## 5. Roles And Permissions

Current roles:

- `STUDENT`
- `SUBJECT_TEACHER`
- `CLASS_TEACHER`
- `HOD`
- `ADMIN`

Current permissions:

- `CREATE_SESSION`
- `LOCK_SESSION`
- `APPROVE_SESSION`
- `CANCEL_SESSION`
- `MANUAL_MARK_ATTENDANCE`
- `VIEW_OWN_ATTENDANCE`
- `VIEW_SECTION_ATTENDANCE`
- `VIEW_DEPARTMENT_ANALYTICS`
- `MANAGE_USERS`
- `ASSIGN_TEACHER_SECTION`
- `ASSIGN_CLASS_TEACHER`

Permission mapping:

- `STUDENT`: `VIEW_OWN_ATTENDANCE`
- `SUBJECT_TEACHER`: session creation/lifecycle, manual attendance, section attendance report
- `CLASS_TEACHER`: same permissions as `SUBJECT_TEACHER`
- `HOD`: department analytics, section attendance report, teacher-section assignment, class-teacher assignment
- `ADMIN`: all permissions

## 6. Domain Model

- `User`: account record with username, email, BCrypt password, role, enabled/email flags, created time, password-change time, registered device ID, failed login/lockout state, last login, and legacy Android-ID/block fields.
- `Student`: preloaded student profile linked one-to-one to `User`, identified by `studentId`, belongs to `Section`, stores optional Base64 X.509 EC public key.
- `Teacher`: preloaded teacher profile linked one-to-one to `User`, identified by `teacherId`, mapped many-to-many to `Subject` and `Section`.
- `Section`: section name and department name.
- `Subject`: unique subject name and many-to-many mapped sections.
- `Room`: room number primary key, unique beacon UUID, dimensions, and safe radius.
- `Session`: attendance session with teacher, subject, section, room, unique 6-digit code, status, date, start/expiry/lock/approval/cancel timestamps.
- `AttendanceBuffer`: provisional attendance mark for a session/student with `AUTO` or `MANUAL` mark type.
- `Attendance`: final approved attendance row for a session/student with `PRESENT` or `ABSENT`.
- `UserSession`: refresh-token session with hashed refresh token, device ID, IP, user agent, timestamps, and revocation flag.
- `DeviceChangeRequest`: user device-change request with old/new device IDs, reason, status, resolver, timestamps, and remarks.
- `EmailVerificationToken`: hashed registration OTP for a college ID.
- `PasswordResetToken`: hashed password-reset token linked to a user.
- `AuditLog`: action, actor, target entity/id, timestamp, and details.

Enums:

- `Role`: `STUDENT`, `SUBJECT_TEACHER`, `CLASS_TEACHER`, `HOD`, `ADMIN`
- `Permission`: the 11 permissions listed above
- `SessionStatus`: `ACTIVE`, `LOCKED`, `APPROVED`, `CANCELLED`
- `AttendanceStatus`: `PRESENT`, `ABSENT`
- `MarkType`: `AUTO`, `MANUAL`
- `DeviceChangeStatus`: `PENDING`, `APPROVED`, `REJECTED`

## 7. Implemented Features

- OTP registration for preloaded student and teacher profiles.
- Staff/student-specific login endpoints plus general login.
- JWT access tokens with persistent refresh-token sessions.
- Refresh-token rotation.
- Logout by refresh token.
- Account lockout for 15 minutes after 5 failed logins.
- Password reset by email OTP.
- JWT/session revocation and device-ID enforcement.
- Admin bootstrap account creation or repair on startup.
- SMTP email service and NoOp fallback email service.
- Teacher department, section, and subject lookup from mappings.
- Session creation by mapped teachers.
- Room lookup by room number or beacon UUID.
- Active-session lookup for teachers and students.
- Student attendance marking with beacon verification, first-time public-key registration, ECDSA signature verification, device ownership checks, and duplicate mark prevention.
- Manual attendance marking by owning teacher.
- Session lock, approval, and cancellation.
- Attendance finalization from buffer to final rows.
- Student own attendance report.
- Section/subject attendance report.
- Department analytics report.
- HOD teacher list with mappings.
- HOD teacher-section assignment, class-teacher assignment, subject-section assignment, and subject-section removal.
- Admin teacher list.
- Admin role assignment for teacher-linked users.
- Admin device reset.
- Student device-change request and admin approval/rejection.
- Scheduled active-session expiry and locked-session cancellation.
- Health endpoint.
- Admin-only password diagnostic endpoint.

## 8. Current Behavior Details

### Registration

Registration starts from an existing `Teacher` or `Student` row. `AuthService.initiateRegistration` looks up the submitted college ID as a teacher ID first and then as a student ID. It rejects missing profiles, profiles already linked to a user, and profiles without email. It stores a hashed six-digit OTP for 10 minutes and sends the raw OTP by email.

`completeRegistration` validates the OTP and college ID, creates a linked `User`, and assigns `SUBJECT_TEACHER` for teacher profiles or `STUDENT` for student profiles.

### Login And Sessions

`AuthService.loginInternal` handles all three login routes. It can enforce student-only or staff-only role restrictions depending on the route. On successful login it revokes all active sessions for that user, creates a new `UserSession`, stores the submitted `deviceId`, and returns an access JWT plus raw refresh token.

Refresh token rotation revokes the old session and creates a new session preserving the previous device ID. Logout revokes the matching refresh-token session if one exists.

### Session Creation

`SessionService.createSession` requires:

- Authenticated user has a teacher profile.
- Teacher is mapped to the subject and section.
- Subject is mapped to the section.
- Teacher has no `ACTIVE` or `LOCKED` session.
- Section has no `ACTIVE` or `LOCKED` session.
- Room exists by room number or beacon UUID.
- Room has beacon UUID, positive safe radius, and positive dimensions.
- Room has no `ACTIVE` or `LOCKED` session.

It creates an `ACTIVE` session with a unique 6-digit code and 60-minute expiry.

The Sunday restriction, 07:00-23:00 creation window, and one-subject/section-per-day rule are present only as commented-out code and are inactive.

### Attendance Marking

`POST /student/attendance` uses `SessionService.markAttendance`.

The active checks require:

- Active session found by six-digit session code.
- Session not expired.
- Current user has a student profile.
- Student belongs to the session section.
- Request beacon UUID matches the session room beacon UUID.
- First device binding does not reuse another user's registered device ID.
- First public key does not match another student's public key.
- First public key is valid Base64 X.509 EC format.
- First attendance signature verifies with the submitted public key before the key/device binding is saved.
- Later attendance uses the stored public key and rejects registered-device mismatch.
- Student has not already marked in the session.

Successful student attendance writes `AttendanceBuffer` with `MarkType.AUTO`. Final `Attendance` rows are not created until session approval.

Android-ID blocking/proxy-detection logic is commented out and inactive. The DTO still accepts `androidId`, and entities still have Android-ID/block fields.

### Session Approval

Locking changes only `ACTIVE` sessions to `LOCKED`.

Approval requires:

- Session is `LOCKED`.
- The authenticated teacher owns the session.

Approval creates one final `Attendance` row for every student in the section. Students in the buffer become `PRESENT`; others become `ABSENT`. The buffer is deleted and the session becomes `APPROVED`.

Cancellation requires `LOCKED`, deletes the buffer, and marks the session `CANCELLED`.

### Reports

Student own report returns approved attendance by subject for the authenticated student and skips subjects with no approved sessions.

Section report returns each student in a section with present count, total approved sessions for the subject/section, and percentage.

Department report loads sections by exact department name, then returns per-section student counts and per-subject approved-session attendance averages. Subjects with no approved sessions are skipped.

### Device Change

Students can submit a device-change request only when they already have a registered device. Duplicate pending requests and same-device requests are rejected.

Admin approval sets the user's registered device ID to the requested new ID, clears linked student public key, revokes active sessions, and marks the request `APPROVED`. Rejection marks the request `REJECTED`.

## 9. Scheduled Jobs

`DemoApplication` enables scheduling.

`SessionExpiryScheduler` runs:

- Every 60 seconds: `expireActiveSessions` changes `ACTIVE` sessions whose `expiryTime` is before now to `LOCKED`.
- Every 300 seconds: `autoCancelLockedSessions` changes `LOCKED` sessions whose `lockedAt` is older than 1 hour to `CANCELLED` and deletes their buffers.

## 10. Email Behavior

`EmailConfiguration` creates:

- `SmtpEmailService` when `spring.mail.host` property is present.
- `NoOpEmailService` when no `EmailService` bean exists.

`SmtpEmailService` sends registration and password-reset OTP messages with `JavaMailSender`. SMTP send failures throw `ServiceUnavailableException`, which is returned as HTTP 503 with a generic unavailable message.

`NoOpEmailService` logs that email is not configured and throws `CustomException`.

## 11. Exception Handling

The global handler returns structured `ErrorResponse` bodies. Expected business errors from `CustomException` and invalid state errors return HTTP 400. Access denied returns 403. Optimistic lock failures return 409. SMTP service failures return 503. Other runtime exceptions return 500 and are logged.

## 12. Testing

Current tests under `src/test/java`:

- `DemoApplicationTests`: context load.
- `TestPasswordSecurityTest`: `/test-password` is denied without auth, forbidden for `ROLE_STUDENT`, and allowed for `ROLE_ADMIN`.
- `DeviceIdSecurityTest`: login persists device ID in `UserSession` and JWT, and device-ID mismatch between JWT and stored session is rejected.

`src/test/resources/application-test.properties` uses H2 in MySQL mode, `ddl-auto=create-drop`, test JWT settings, and local mail properties.

## 13. Known Gaps Visible In Current Code

- No active controller provides admin CRUD for creating users, students, teachers, subjects, sections, rooms, or bulk master data.
- `seed-data.ps1` exists and calls `POST /admin/users`, but no active controller maps `/admin/users`.
- Android-ID blocking and admin unblock endpoints are present only as commented-out code.
- Some legacy Android-ID/blocking fields remain on `User` and `AttendanceBuffer`.
- `AdminController.assignRole` writes the audit log with the target user as actor because the service method receives only `userId` and role.
- `TestController` is an active admin-only diagnostic endpoint that returns submitted password/hash values and a newly generated hash.

## 14. Supporting Files

- `pom.xml`: Maven project and dependencies.
- `src/main/resources/application.properties`: environment-driven runtime config.
- `src/test/resources/application-test.properties`: test profile config.
- `Procfile`: runs `java -jar target/demo-0.0.1-SNAPSHOT.jar`.
- `seed-data.ps1`: existing PowerShell seed script; it includes direct SQL seeding and stale calls to inactive `POST /admin/users`.
- `seed-output.txt`: existing captured seed output file.

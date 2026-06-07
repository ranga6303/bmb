# System Flow - Attendance System API

This document describes the current runtime flows implemented in `src/main/java/com/example/demo`.

## 1. Authentication And Authorization Boundary

Request path:

```text
Client -> SecurityConfig -> JwtAuthenticationFilter -> Controller -> Service
```

Public boundary:

1. `SecurityConfig` permits `/auth/**`, `/`, and `/health`.
2. `GET /health` is mapped by `HealthC`.
3. `/` is permitted by security config but has no mapped controller in the current code.

Authenticated boundary:

1. Every other route requires authentication.
2. Controllers then enforce permissions or roles with `@PreAuthorize`.
3. `CustomUserDetailsService` grants both `ROLE_<role>` and each permission from `Role.getPermissions()`.

JWT/session validation:

1. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`.
2. It extracts the username from the JWT.
3. It loads user details from `CustomUserDetailsService`.
4. `JwtUtil.validateToken` checks username, token expiry, and whether token issue time is after `lastPasswordChange` when that field exists.
5. The filter extracts `sessionId` from the JWT.
6. It loads `UserSession` by ID.
7. The session must exist and must not be revoked.
8. If the stored `UserSession.deviceId` is not null, it must equal the JWT `deviceId` claim.
9. The filter builds the Spring Security authentication context with the user's authorities.

## 2. Registration Flow

Request path:

```text
Client -> AuthController -> AuthService -> Teacher/Student repository -> token repository -> EmailService
```

Initiation:

1. Client calls `POST /auth/initiate-registration` with `collegeId`.
2. `AuthService` trims the college ID.
3. It searches `TeacherRepository.findByTeacherId(collegeId)`.
4. If no teacher is found, it searches `StudentRepository.findByStudentId(collegeId)`.
5. It rejects unknown college IDs.
6. It rejects profiles already linked to a `User`.
7. It rejects profiles with no email.
8. It deletes existing registration tokens for the college ID.
9. It generates a 6-digit OTP.
10. It stores only the SHA-256 hash of the OTP in `EmailVerificationToken`.
11. The token stores the college ID, a 10-minute expiry, `used=false`, and no linked user.
12. `EmailService.sendOtpEmail` sends the raw OTP.
13. The response message includes a masked email address.

Completion:

1. Client calls `POST /auth/complete-registration` with `collegeId`, `otp`, and `newPassword`.
2. The OTP is hashed and looked up as an unused `EmailVerificationToken`.
3. The token must not be expired.
4. The token college ID must equal the request college ID.
5. The service again finds the matching teacher or student profile.
6. It rejects unknown college IDs and profiles already linked to a user.
7. It creates a new `User` with username equal to college ID, encoded password, enabled flag true, and email verified true.
8. Teacher profiles create a `SUBJECT_TEACHER` user linked to the teacher.
9. Student profiles create a `STUDENT` user linked to the student.
10. The verification token is marked used.

## 3. Login, Refresh, And Logout Flow

Request path:

```text
Client -> AuthController -> AuthService -> AuthenticationManager -> UserSessionRepository -> JwtUtil
```

Login:

1. Client calls one of:
   - `POST /auth/login`
   - `POST /auth/login/student`
   - `POST /auth/login/staff`
2. `/auth/login/student` rejects users whose role is not `STUDENT`.
3. `/auth/login/staff` rejects users whose role is `STUDENT`.
4. Temporarily locked accounts are rejected when `accountLockedUntil` is in the future.
5. Bad credentials increment `failedLoginAttempts`.
6. Five failed attempts set `accountLockedUntil` to 15 minutes in the future.
7. Successful login clears failed attempts and lock time.
8. Successful login sets `lastLoginAt`.
9. All active sessions for the user are revoked.
10. A raw refresh token is generated.
11. Only the refresh token hash is stored.
12. A new `UserSession` stores user, request `deviceId`, IP address, user agent, `lastActiveAt`, and `revoked=false`.
13. `JwtUtil` issues an access JWT with `userId`, `role`, `sessionId`, and `deviceId` claims.
14. A `LOGIN_SUCCESS` audit log is written after the login transaction commits.

Refresh:

1. Client calls `POST /auth/refresh` with `refreshToken`.
2. The raw refresh token is hashed.
3. `UserSessionRepository.findByRefreshTokenHashAndRevokedFalse` loads the active session.
4. The old session is marked revoked.
5. A new raw refresh token is generated and hashed.
6. A new `UserSession` is created for the same user.
7. The new session preserves the old session's `deviceId`.
8. A new access JWT and raw refresh token are returned.

Logout:

1. Client calls `POST /auth/logout` with `refreshToken`.
2. The token is hashed.
3. If a matching active session exists, it is marked revoked.
4. The endpoint returns success even when no matching active session is found.

## 4. Password Reset Flow

Request path:

```text
Client -> AuthController -> AuthService -> profile repository -> PasswordResetTokenRepository -> EmailService
```

Forgot password:

1. Client calls `POST /auth/forgot-password` with `collegeId`.
2. The service trims the college ID.
3. It searches student and teacher profiles.
4. It rejects unknown college IDs.
5. It requires the profile to have a linked `User`.
6. It requires the profile to have an email.
7. Existing password reset tokens for the user are deleted.
8. A 6-digit OTP is generated.
9. The raw token is `collegeId::otp`.
10. The raw token is hashed and saved in `PasswordResetToken`.
11. The reset token expires in 15 minutes.
12. `EmailService.sendPasswordResetOtp` sends the raw OTP.

Reset password:

1. Client calls `POST /auth/reset-password` with `collegeId`, `otp`, and `newPassword`.
2. The controller builds `collegeId::otp`.
3. The service hashes it and loads the `PasswordResetToken`.
4. The token must be unused and unexpired.
5. The user's password is replaced with a BCrypt hash of the new password.
6. `lastPasswordChange` is set to now.
7. The token is marked used.
8. Other reset tokens for the same user are deleted.

## 5. Teacher Session Creation Flow

Request path:

```text
Teacher -> SessionController -> SessionService -> RoomService -> repositories
```

Flow:

1. Teacher calls `POST /sessions`.
2. Request validation requires `subjectId`, `sectionId`, and either `roomNumber` or `beaconUuid`.
3. `CurrentUserService` provides the authenticated user.
4. `SessionService` loads the teacher profile linked to that user.
5. It loads the requested `Subject` and `Section`.
6. The teacher must be mapped to both the subject and section.
7. The subject must be mapped to the section.
8. The teacher must not already have an `ACTIVE` or `LOCKED` session.
9. The section must not already have an `ACTIVE` or `LOCKED` session.
10. `RoomService` resolves the room by room number when provided, otherwise by beacon UUID.
11. The room must have a beacon UUID.
12. The room must have positive safe radius, length, and width.
13. The room must not have another `ACTIVE` or `LOCKED` session.
14. A unique 6-digit session code is generated.
15. The session is saved with today's date, current start time, 60-minute expiry, and `ACTIVE` status.
16. `SESSION_CREATED` is written to `AuditLog`.

Inactive code notes:

- Sunday session blocking is commented out.
- The 07:00-23:00 creation window is commented out.
- The one-session-per-subject/section/day rule is commented out.

## 6. Student Attendance Marking Flow

Request path:

```text
Student -> StudentController -> SessionService -> DeviceVerificationService -> AttendanceBufferRepository
```

Shared validations:

1. Student calls `POST /student/attendance`.
2. The request must include a 6-digit `sessionCode`, `beaconUuid`, `deviceId`, `deviceSignature`, and `signedPayload`.
3. `SessionService` finds an `ACTIVE` session by session code.
4. The session must not be expired.
5. The authenticated user must have a linked student profile.
6. The student's section must match the session section.
7. Request beacon UUID must equal the session room beacon UUID.
8. When the user has no registered device ID, the request device ID must not already be registered to another user.

First-time public-key/device registration:

1. This path runs when `Student.publicKey` is null and request `publicKey` is not null.
2. The submitted public key must not already belong to another student.
3. `DeviceVerificationService.registerPublicKey` Base64-decodes the key and validates it as an EC X.509 public key.
4. The validated key is set on the student object.
5. Before saving the user or student, `verifySignatureWithKey(request.publicKey, request)` verifies the ECDSA signature.
6. Signature verification uses `SHA256withECDSA`, the submitted public key, `signedPayload` bytes, and Base64-decoded `deviceSignature`.
7. If verification fails, a `CustomException` stops the flow before persistence.
8. If verification passes and `User.registeredDeviceId` is null, the request device ID is stored on the user.
9. The student public key is saved.

Returning device:

1. This path runs when `Student.publicKey` is already present.
2. If `User.registeredDeviceId` and request `deviceId` are both present, they must match.
3. `DeviceVerificationService.verifySignature` verifies the signature with the stored student public key.

Missing key:

1. If the student has no stored public key and the request has no public key, the service rejects the mark.

Buffer write:

1. Duplicate buffer marks for the same session/student are rejected.
2. A new `AttendanceBuffer` is saved with the session, student, and `MarkType.AUTO`.

Inactive code notes:

- Android-ID fields and logic have been removed.

## 7. Manual Attendance Flow

Request path:

```text
Teacher -> SessionController -> SessionService -> AttendanceBufferRepository
```

Flow:

1. Teacher calls `POST /sessions/{id}/manual?studentId=...`.
2. The caller must have `MANUAL_MARK_ATTENDANCE`.
3. The session must exist.
4. The session must not be `APPROVED` or `CANCELLED`.
5. The session must be `ACTIVE`.
6. The authenticated user must have a teacher profile.
7. The authenticated teacher must own the session.
8. The target student must exist.
9. The student must belong to the session section.
10. Duplicate buffer marks are rejected.
11. `AttendanceBuffer` is saved with `MarkType.MANUAL`.
12. `MANUAL_ATTENDANCE` is written to `AuditLog`.

## 8. Lock, Approve, And Cancel Flow

Lock:

1. Teacher calls `POST /sessions/{id}/lock`.
2. The caller must have `LOCK_SESSION`.
3. The session must exist.
4. The session must not be `APPROVED` or `CANCELLED`.
5. The session must be `ACTIVE`.
6. Status becomes `LOCKED`.
7. `lockedAt` is set to now.
8. `SESSION_LOCKED` is written to `AuditLog`.

Approve:

1. Teacher calls `POST /sessions/{id}/approve`.
2. The caller must have `APPROVE_SESSION`.
3. The session must exist.
4. The session must not be `APPROVED` or `CANCELLED`.
5. The session must be `LOCKED`.
6. The authenticated user must have a teacher profile.
7. The authenticated teacher must own the session.
8. Buffer rows are loaded for the session.
9. Buffer marks are indexed by student ID.
10. All students in the session section are loaded.
11. Each student gets a final `Attendance` row.
12. Buffered students are marked `PRESENT`.
13. Students not in the buffer are marked `ABSENT`.
14. Session status becomes `APPROVED`.
15. `approvedAt` is set to now.
16. The attendance buffer for the session is deleted.
17. `SESSION_APPROVED` is written to `AuditLog`.

Cancel:

1. Teacher calls `POST /sessions/{id}/cancel`.
2. The caller must have `CANCEL_SESSION`.
3. The session must exist.
4. The session must not be `APPROVED` or `CANCELLED`.
5. The session must be `LOCKED`.
6. The attendance buffer for the session is deleted.
7. Session status becomes `CANCELLED`.
8. `cancelledAt` is set to now.
9. `SESSION_CANCELLED` is written to `AuditLog`.

Terminal status rule:

- `SessionService.validateNotTerminal` rejects later operations on `APPROVED` and `CANCELLED` sessions.

## 9. Active Session Lookup Flows

Teacher active session:

1. Teacher calls `GET /sessions/active`.
2. The caller must have `CREATE_SESSION`.
3. The current user must have a teacher profile.
4. The service searches for the teacher's `ACTIVE` or `LOCKED` session.
5. If found, the response contains `sessionId`, `subjectName`, `sectionName`, and `status`.
6. If none exists, the controller returns HTTP 204.

Student active session:

1. Student calls `GET /student/active-session`.
2. The caller must have `VIEW_OWN_ATTENDANCE`.
3. The current user must have a student profile.
4. The service searches for an `ACTIVE` session in the student's section.
5. Missing active sessions are rejected.
6. Expired active sessions are rejected.
7. The response contains session ID, session code, subject name, teacher name, room number, beacon UUID, and expiry time.

## 10. Reporting Flows

Student own attendance report:

1. Student calls `GET /student/my-attendance`.
2. The caller must have `VIEW_OWN_ATTENDANCE`.
3. The current user must have a student profile.
4. The service loads the student's section.
5. The service loads all subjects.
6. Present attendance rows for the student are counted by subject for approved sessions.
7. For each subject, approved sessions for the student's section are counted.
8. Subjects with zero approved sessions for that section are skipped.
9. The response includes attended count, total approved sessions, and percentage per included subject.

Section attendance report:

1. Authorized user calls `GET /sections/{id}/attendance?subjectId=...`.
2. The caller must have `VIEW_SECTION_ATTENDANCE`.
3. The service loads section and subject.
4. It counts approved sessions for that section/subject.
5. It counts present attendance grouped by student for approved sessions.
6. It loads all students in the section.
7. Each student receives attended count, total approved sessions, and percentage.
8. Percentage is `0.0` when total approved sessions is zero.

Expanded department report:

1. Authorized user calls `GET /departments/{name}/report`.
2. The caller must have `VIEW_DEPARTMENT_ANALYTICS`.
3. The controller loads sections by exact `departmentName`.
4. It counts total sections from the returned section list.
5. For each section, it counts students using `studentRepository.countBySectionId`.
6. Top-level `totalStudents` is the sum of section student counts.
7. It loads subjects mapped to the section with `subjectRepository.findByMappedSectionsContaining`.
8. For each mapped subject, it counts approved sessions for subject/section.
9. Subjects with zero approved sessions are skipped.
10. It loads present counts from approved present attendance rows using `attendanceRepository.countPresentByStudent`.
11. It sums all present counts returned by the query.
12. Total possible attendance is `approvedSessions * sectionStudentCount`.
13. Average attendance percentage is `(totalPresent * 100.0) / totalPossible`, or `0.0` when total possible is zero.
14. The response shape is department -> sections -> subjects with section counts and subject averages.

## 11. Device Change Flow

Student request:

1. Student calls `POST /student/device-change-request`.
2. The caller must have `VIEW_OWN_ATTENDANCE`.
3. The current user must already have `registeredDeviceId`.
4. The requested `newDeviceId` must differ from the current registered device ID.
5. The user must not already have a pending device-change request.
6. A `DeviceChangeRequest` is saved with old device ID, new device ID, optional reason, `PENDING` status, and request time.

Student request history:

1. Student calls `GET /student/device-change-requests`.
2. The caller must have `VIEW_OWN_ATTENDANCE`.
3. The service returns all device-change requests for the current user.
4. The controller returns ID, old/new device IDs, reason, status, request time, and admin remarks.

Admin pending list:

1. Admin calls `GET /admin/device-change-requests`.
2. The caller must have `MANAGE_USERS`.
3. The service returns only `PENDING` requests.
4. The controller returns request ID, user ID, username, old/new device IDs, reason, and request time.

Admin approval:

1. Admin calls `POST /admin/device-change-requests/{id}/approve`.
2. The caller must have `MANAGE_USERS`.
3. Optional body may include `adminRemarks`.
4. The request must exist and be `PENDING`.
5. The user's `registeredDeviceId` is set to the requested new device ID.
6. If the user has a linked student profile, that student's public key is cleared.
7. Active sessions for the user are revoked.
8. Request status becomes `APPROVED`.
9. `resolvedAt`, `resolvedBy`, and optional admin remarks are stored.

Admin rejection:

1. Admin calls `POST /admin/device-change-requests/{id}/reject`.
2. The caller must have `MANAGE_USERS`.
3. Optional body may include `adminRemarks`.
4. The request must exist and be `PENDING`.
5. Request status becomes `REJECTED`.
6. `resolvedAt`, `resolvedBy`, and optional admin remarks are stored.

## 12. HOD Mapping And Teacher List Flows

HOD teacher list:

1. HOD/admin calls `GET /hod/teachers`.
2. The caller must have `ASSIGN_TEACHER_SECTION`.
3. `TeacherRepository.findAllWithMappings` fetches teachers with linked user, mapped sections, and mapped subjects.
4. Each response item includes teacher ID, name, email, role or `NOT_REGISTERED`, mapped section chips, and mapped subject chips.

Assign section to teacher:

1. HOD/admin calls `POST /hod/teachers/{teacherId}/assign-section`.
2. The caller must have `ASSIGN_TEACHER_SECTION`.
3. Teacher is loaded by `teacherId`.
4. Section is loaded by request `sectionId`.
5. The section is added to the teacher's mapped sections.
6. The teacher is saved.

Assign class teacher:

1. HOD/admin calls `POST /hod/sections/{sectionId}/assign-class-teacher`.
2. The caller must have `ASSIGN_CLASS_TEACHER`.
3. Teacher is loaded by request `teacherId`.
4. Teacher must have a linked user with role `CLASS_TEACHER`.
5. Section is loaded by path `sectionId`.
6. The section must not already have a mapped teacher whose user role is `CLASS_TEACHER`.
7. The section is added to the teacher's mapped sections.
8. The teacher is saved.

Assign subject to section:

1. HOD/admin calls `POST /hod/sections/{sectionId}/assign-subject?subjectId=...`.
2. The caller must have `ASSIGN_TEACHER_SECTION`.
3. Section and subject are loaded.
4. Existing subject-section mapping is rejected.
5. The section is added to the subject's mapped sections.
6. The subject is saved.

Remove subject from section:

1. HOD/admin calls `POST /hod/sections/{sectionId}/remove-subject?subjectId=...`.
2. The caller must have `ASSIGN_TEACHER_SECTION`.
3. Section and subject are loaded.
4. Missing subject-section mapping is rejected.
5. The section is removed from the subject's mapped sections.
6. The subject is saved.

## 13. Background Jobs

Scheduler path:

```text
Spring scheduler -> SessionExpiryScheduler -> SessionService
```

Auto-lock expired active sessions:

1. Runs every 60 seconds.
2. Finds `ACTIVE` sessions whose `expiryTime` is before now.
3. Each found session becomes `LOCKED`.
4. `lockedAt` is set to now.
5. The scheduler logs when at least one session is auto-expired.

Auto-cancel old locked sessions:

1. Runs every 300 seconds.
2. Computes one hour before now.
3. Finds `LOCKED` sessions whose `lockedAt` is before that timestamp.
4. Deletes each session's attendance buffer.
5. Each found session becomes `CANCELLED`.
6. `cancelledAt` is set to now.
7. The scheduler logs when at least one session is auto-cancelled.

## 14. Admin Device, Role, And Staff List Operations

Admin staff/teacher list:

1. Admin calls `GET /admin/teachers`.
2. The caller must have `MANAGE_USERS`.
3. `TeacherRepository.findAll()` returns all teachers.
4. For each teacher, the controller reads the linked user if present.
5. Each response item includes teacher ID, name, email, linked user ID or null, role name or `NOT_REGISTERED`, and registered device ID or null.
6. This endpoint is the backend staff list source for admin staff-management UI.

Role assignment:

1. Admin calls `POST /admin/users/{userId}/role`.
2. The caller must have `MANAGE_USERS`.
3. Request role must not be null.
4. Role `STUDENT` cannot be assigned through this endpoint.
5. The target user must exist.
6. The target user must be linked to a teacher profile.
7. The user's role is updated.
8. `ASSIGN_ROLE` is written to `AuditLog`.

Device reset:

1. Admin calls `POST /admin/reset-device/{userId}`.
2. The caller must have `MANAGE_USERS`.
3. The target user must exist.
4. `User.registeredDeviceId` is cleared.
5. If the user is linked to a student profile, `Student.publicKey` is cleared.
6. Active sessions for the user are revoked.
7. `DEVICE_RESET` is written to `AuditLog`.

Inactive admin blocking flows:

- `GET /admin/blocked-students` and `POST /admin/unblock/{userId}` exist only inside commented-out code in `AdminController`.
- They are not active endpoints.

# Business Flows

## Registration Flow

1. Client calls `AuthController.initiateRegistration()` with `collegeId`.
2. `AuthService.initiateRegistration()` looks for a matching `Teacher.teacherId` first, then `Student.studentId`.
3. If a linked `User` already exists, registration is rejected.
4. The service deletes older `EmailVerificationToken` rows for that `collegeId`, creates a six-digit OTP, hashes it with `TokenHashUtil.hashToken()`, stores the hash, and sends the OTP through `EmailService.sendOtpEmail()`.
5. Client calls `AuthController.completeRegistration()` with `collegeId`, `otp`, and `newPassword`.
6. `AuthService.completeRegistration()` hashes the OTP, validates token existence, expiry, and college ID.
7. A new `User` is created with a BCrypt password. Teacher accounts default to `Role.SUBJECT_TEACHER`; student accounts use `Role.STUDENT`.
8. The new `User` is linked back to the matching `Teacher` or `Student`.

## Login And Refresh Flow

1. Client calls `/auth/login`, `/auth/login/student`, or `/auth/login/staff`.
2. `AuthService.loginInternal()` loads the `User`, checks endpoint-role compatibility, and rejects currently locked accounts.
3. Spring `AuthenticationManager` validates the password using `CustomUserDetailsService` and BCrypt.
4. Failed logins call `incrementFailed()`; after five failures the user is locked for 15 minutes.
5. Successful login resets failures, updates `lastLoginAt`, revokes all active sessions for the user, creates a new `UserSession`, and returns an access JWT plus raw refresh token.
6. The JWT includes `userId`, `role`, `sessionId`, and `deviceId` claims.
7. `/auth/refresh` hashes the refresh token, finds an active `UserSession`, revokes it, creates a new session, and returns rotated tokens.

## JWT Request Authentication Flow

1. `JwtAuthenticationFilter.doFilterInternal()` reads the `Authorization` header.
2. `JwtUtil.extractUsername()` parses and verifies the signed token.
3. `CustomUserDetailsService.loadUserByUsername()` loads role and permission authorities.
4. `JwtUtil.validateToken()` checks username, expiry, and rejects tokens issued at or before `User.lastPasswordChange`.
5. The filter loads `UserSession` by token `sessionId`.
6. The request is authenticated only if the session exists, is not revoked, and the stored session device ID matches the JWT device ID when a device ID exists.

## Session Creation Flow

1. A teacher/HOD/class teacher calls `SessionController.create()`.
2. `SessionService.createSession()` loads the current actor's `Teacher` profile, target `Subject`, and target `Section`.
3. The teacher must have a `TeacherSectionSubject` row for that subject and section.
4. The service rejects another `ACTIVE` or `LOCKED` session for the same teacher or section.
5. `RoomService.getRoomForSession()` resolves by `roomNumber` or `beaconUuid`, validates room beacon/dimensions/radius, and rejects rooms with active or locked sessions.
6. A unique six-digit `sessionCode` is generated and a `Session` is saved with status `ACTIVE`, `startTime`, and `expiryTime`.
7. `SessionService.persistAudit()` writes `SESSION_CREATED`.

Note: the code currently sets `expiryTime` to `now.plusMinutes(60)`. Comments and README references to a three-minute window do not match the implementation.

## Student Attendance Marking Flow

1. Student calls `StudentController.markAttendance()` with `MarkAttendanceRequest`.
2. `SessionService.markAttendance()` finds an `ACTIVE` session by `sessionCode` and rejects expired sessions.
3. The current user must have a `Student` profile and belong to the session's section.
4. Request `beaconUuid` must exactly match `session.room.beaconUuid`.
5. `handleDeviceBinding()` enforces one device per user:
   - First mark with no registered device binds `User.registeredDeviceId`.
   - If another user already owns that device ID, the mark is rejected.
   - If a different device is used later, the mark is rejected.
6. If `publicKey` is supplied, `DeviceVerificationService.registerPublicKey()` validates it as an EC key, `validateSignedPayload()` checks the payload format, and `verifySignatureWithKey()` verifies `SHA256withECDSA`.
7. If no `publicKey` is supplied but the student already has one, the signature is verified using the stored key.
8. Duplicate marks are blocked by checking `AttendanceBufferRepository.existsBySessionAndStudent()`.
9. An `AttendanceBuffer` row is saved with `MarkType.AUTO`.

## Lock, Approve, Cancel Flow

1. Owning teacher calls `/sessions/{id}/lock`; `SessionService.lockSession()` requires status `ACTIVE` and changes it to `LOCKED`.
2. Owning teacher calls `/sessions/{id}/approve`; `SessionService.approveSession()` requires status `LOCKED`.
3. Approval loads all `AttendanceBuffer` rows for the session and all students in the section.
4. For every section student, an `Attendance` row is written as `PRESENT` if in the buffer, otherwise `ABSENT`.
5. Session status changes to `APPROVED`, buffer rows are deleted, and `SESSION_APPROVED` audit is written.
6. Owning teacher can cancel only `LOCKED` sessions. Cancellation deletes buffer rows and sets status `CANCELLED`.

## Device Change Flow

1. Student calls `StudentController.requestDeviceChange()` with `newDeviceId` and optional reason.
2. `DeviceChangeService.submitRequest()` rejects duplicate pending requests and stores a `DeviceChangeRequest` with status `PENDING`.
3. Admin calls approve or reject under `/admin/device-change-requests/{id}`.
4. Approval sets `User.registeredDeviceId` to the requested new device ID, clears `Student.publicKey`, revokes active sessions, and marks the request `APPROVED`.
5. Rejection marks the request `REJECTED` and stores optional admin remarks.

## Reporting Flow

| Report | Entry point | Implementation |
|---|---|---|
| Student own attendance | `StudentController.myAttendance()` | `SessionService.getOwnAttendanceReport()` counts approved sessions and present rows per subject. |
| Section attendance | `SectionController.attendance()` | `SessionService.getSectionSubjectReport()` returns per-student totals for a section/subject. |
| Department report | `HodController.departmentReport()` or `DepartmentController.report()` | Counts sections, students, approved sessions, and average attendance per subject. |


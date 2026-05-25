# System Flow - Attendance System API

This document summarizes the main system flows in the current codebase.

## 1. Registration Flow (Teacher-Centric OTP)

User -> AuthController -> AuthService -> EmailVerificationTokenRepository -> EmailService -> Response

Flow detail:

1) POST /auth/initiate-registration
2) Validate teacherId and email
3) Create EmailVerificationToken (hashed OTP, 10-minute expiry)
4) Send OTP email
5) POST /auth/complete-registration
6) Validate OTP + create User + link Teacher (role SUBJECT_TEACHER)

Missing/partial links:

- Only teachers can register; students are not supported in this flow.

## 2. Login and Token Flow

User -> AuthController -> AuthService -> AuthenticationManager -> UserSessionRepository -> JwtUtil -> Response

Flow detail:

1) POST /auth/login (or /auth/login/student, /auth/login/staff)
2) Authenticate credentials; lock account after 5 failed attempts
3) Revoke all previous sessions
4) Create UserSession with hashed refresh token
5) Issue access token and refresh token (sessionId claim included)
6) JwtAuthenticationFilter validates token, session, and deviceId match when a session deviceId is present

Missing/partial links:

- Login ignores deviceId; UserSession.deviceId is set to null, so JWT device binding is not enforced.

## 3. Password Reset Flow

User -> AuthController -> AuthService -> PasswordResetTokenRepository -> EmailService -> Response

Flow detail:

1) POST /auth/forgot-password
2) Validate collegeId and linked user account
3) Generate 6-digit OTP, hash "collegeId::otp", store in PasswordResetToken (15-minute expiry)
4) Send OTP email
5) POST /auth/reset-password
6) AuthService.resetPassword hashes "collegeId::otp" and updates the password

## 4. Logout Flow

User -> AuthController -> AuthService -> UserSessionRepository -> Response

Flow detail:

1) POST /auth/logout (with refresh token)
2) Hash refresh token and revoke matching UserSession

## 5. Attendance Session Creation

Teacher -> SessionController -> SessionService -> RoomService -> SessionRepository -> Response

Flow detail:

1) Validate teacher mappings to section and subject
2) Validate subject is assigned to the section
3) Validate time window (07:00-21:00) and not Sunday
4) Ensure no duplicate session for subject/section/day and no active/locked session for teacher/section
5) Validate room and ensure it is not occupied
6) Create ACTIVE session with 6-digit code and 10-minute expiry

## 5. Student Attendance Marking

Student -> StudentController -> SessionService -> DeviceVerificationService -> AttendanceBufferRepository -> Response

Flow detail:

1) Validate active session and section, and check expiry
2) Validate beacon UUID matches room
3) If no public key is stored, register public key and bind deviceId on the user
4) If public key exists, enforce registered deviceId match and verify ECDSA signature
5) Save AttendanceBuffer (MarkType.AUTO)

Missing/partial links:

- Legacy endpoint /student/attendance?sessionCode=... skips device verification and beacon validation.
- Signature is not validated on the first public-key registration path.

## 6. Session Lock and Approval

Teacher -> SessionController -> SessionService -> AttendanceBufferRepository -> AttendanceRepository -> Response

Flow detail:

1) Lock ACTIVE session (status -> LOCKED)
2) Approve LOCKED session (owning teacher only)
3) Convert buffer into final Attendance
4) Mark absences and clear buffer
5) Cancel LOCKED session clears buffer and sets status CANCELLED

## 7. Device Change Workflow

Student -> StudentController -> DeviceChangeService -> DeviceChangeRequestRepository -> Response

Admin -> AdminController -> DeviceChangeService -> UserRepository -> UserSessionRepository -> Response

Flow detail:

1) Student requests device change (must already have registeredDeviceId)
2) Admin approves/rejects
3) On approval: update registeredDeviceId, clear student public key, revoke sessions

## 8. HOD Assignment Flows

HOD -> HodController -> TeacherRepository/SectionRepository/SubjectRepository -> Response

Flow detail:

- Assign teacher to section
- Assign class teacher to section
- Assign/remove subject to section

## 9. Reporting

Section report:
User -> SectionController -> SessionService -> AttendanceRepository -> Response

Department report:
User -> DepartmentController -> (placeholder) -> Response

Missing/partial links:

- Department report is not implemented.

## 10. Background Jobs

Scheduler -> SessionService -> SessionRepository -> Response

- Auto-lock expired ACTIVE sessions (every 60 seconds)
- Auto-cancel LOCKED sessions older than 1 hour (every 5 minutes)

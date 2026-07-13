# Security

## Authentication

Authentication is JWT-based and stateless from Spring Security's perspective. `SecurityConfig` disables CSRF, enables CORS, sets `SessionCreationPolicy.STATELESS`, permits `/auth/**`, `/`, and `/health`, and requires authentication for every other request.

`JwtAuthenticationFilter` validates Bearer tokens and only authenticates the request when:

1. JWT signature and expiry are valid.
2. Token username matches `UserDetails`.
3. Token issue time is after `User.lastPasswordChange`, when that timestamp exists.
4. The token contains a `sessionId`.
5. The corresponding `UserSession` exists and is not revoked.
6. If the stored `UserSession.deviceId` is not null, it equals the JWT `deviceId` claim.

## Authorization

Roles are represented by `Role`; each role exposes a set of `Permission` values. `CustomUserDetailsService` grants both `ROLE_<role>` and each permission name as Spring authorities.

| Role | Permissions |
|---|---|
| `STUDENT` | `VIEW_OWN_ATTENDANCE` |
| `SUBJECT_TEACHER` | create/lock/approve/cancel sessions, manual mark, view section attendance |
| `CLASS_TEACHER` | same as subject teacher |
| `HOD` | department analytics, section attendance, teacher/class-teacher assignment, session operations |
| `ADMIN` | all permissions |

Controllers use `@PreAuthorize` with permission authorities such as `MANAGE_USERS`, `CREATE_SESSION`, and `VIEW_OWN_ATTENDANCE`.

## Public Endpoints

Configured public paths:

| Path | Notes |
|---|---|
| `/auth/**` | Includes login, registration, password reset, refresh, and logout. |
| `/health` | Plain health check. |
| `/` | Permitted, but no controller was found for `/`. |

## Protected Endpoint Groups

| Group | Protection |
|---|---|
| `/student/**` | `VIEW_OWN_ATTENDANCE`. |
| `/sessions/**` | Session permissions: create, lock, approve, cancel, manual mark. |
| `/teacher/**` | Teacher/HOD/class-teacher permissions depending on endpoint. |
| `/sections/**` | `VIEW_SECTION_ATTENDANCE`. |
| `/hod/**` | HOD assignment and analytics permissions. |
| `/departments/**` | `VIEW_DEPARTMENT_ANALYTICS`. |
| `/admin/**` | `MANAGE_USERS`. |
| `/test-password` | `ROLE_ADMIN`. |
| `/greet` | Any authenticated user. |

## Token Handling

| Token | Storage/handling |
|---|---|
| Access JWT | Returned raw to client; signed HS256 using `app.jwt.secret`; includes `userId`, `role`, `sessionId`, `deviceId`. |
| Refresh token | Returned raw once; stored only as SHA-256 hash in `user_sessions.refreshTokenHash`. |
| Registration OTP | Six-digit OTP emailed; SHA-256 hash stored in `email_verification_tokens`. |
| Password reset OTP | Six-digit OTP emailed; hash of `collegeId::otp` stored in `password_reset_tokens`. |

Login revokes all prior sessions for the user. Refresh token rotation revokes the old session and creates a new one.

## Device And Attendance Security

`SessionService.markAttendance()` enforces:

1. Active session code.
2. Unexpired session.
3. Student must belong to session section.
4. Request beacon UUID must match the room beacon UUID.
5. Registered device ID must match after first binding.
6. Duplicate buffer marks are rejected.
7. EC public key/signature is verified when a key is supplied or already registered.

`DeviceVerificationService` uses Java `Signature` with `SHA256withECDSA` and EC public keys encoded as X.509 Base64.

## CORS

`SecurityConfig.corsConfigurationSource()` allows all origins, methods `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, all headers, and no credentials.

## Exception Handling

`GlobalExceptionHandler` maps validation errors, access denied, custom exceptions, service unavailable, optimistic lock conflicts, and runtime exceptions. The catch-all runtime handler returns `ex.getMessage()` to clients, which can expose internal details.


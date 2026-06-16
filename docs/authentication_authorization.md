# Authentication & Authorization

## Overview

The API uses **stateless JWT authentication** with **server-side refresh token sessions**. Authorization combines Spring Security role authorities (`ROLE_*`) and fine-grained permission authorities derived from the `Role` enum.

---

## Authentication Flow

### Registration (two-step OTP)

```text
1. POST /auth/initiate-registration  { collegeId }
   → Lookup Teacher/Student by college ID
   → Reject if profile missing, already linked to User, or no email
   → Generate 6-digit OTP, store SHA-256 hash in EmailVerificationToken
   → Email raw OTP (10 min expiry)

2. POST /auth/complete-registration  { collegeId, otp, newPassword }
   → Validate OTP hash + college ID match
   → Create User (username = collegeId, BCrypt password)
   → Link to Teacher (role SUBJECT_TEACHER) or Student (role STUDENT)
```

### Login

Three endpoints share `AuthService.loginInternal()`:

| Endpoint | Role restriction |
|----------|------------------|
| `POST /auth/login` | None |
| `POST /auth/login/student` | Rejects non-STUDENT roles |
| `POST /auth/login/staff` | Rejects STUDENT role |

**Login steps:**
1. Validate credentials via `AuthenticationManager`
2. Reject if `accountLockedUntil` is in the future
3. On failure: increment `failedLoginAttempts`; lock at 5 failures for 15 minutes
4. On success: clear failures, set `lastLoginAt`, revoke all active sessions
5. Create new `UserSession` with hashed refresh token
6. Issue JWT with claims: `userId`, `role`, `sessionId`, `deviceId`

> **Known issue:** `LoginRequest.deviceId` is accepted in the DTO but **not used**. JWT `deviceId` comes from `User.registeredDeviceId` (set during attendance marking), not the login request. See [audit_report.md](audit_report.md) finding H-1.

### Refresh Token Rotation

```text
POST /auth/refresh  { refreshToken }
  → Hash token, find active UserSession
  → Revoke old session
  → Create new session (preserves deviceId from old session)
  → Return new access JWT + new raw refresh token
```

### Logout

```text
POST /auth/logout  { refreshToken }
  → Revoke matching session if found
  → Always returns success
```

### Password Reset

```text
POST /auth/forgot-password  { collegeId }
  → Generate OTP, store hash of "collegeId::otp" in PasswordResetToken (15 min)

POST /auth/reset-password  { collegeId, otp, newPassword }
  → Validate token, update password, set lastPasswordChange
  → Invalidates JWTs issued before password change (via JwtUtil.validateToken)
```

---

## JWT Structure

| Claim | Source | Purpose |
|-------|--------|---------|
| `sub` | `user.username` | Identity |
| `userId` | `user.id` | Internal reference |
| `role` | `user.role.name()` | Role name |
| `sessionId` | `UserSession.id` | Session revocation check |
| `deviceId` | `user.registeredDeviceId` | Device binding enforcement |
| `iat` / `exp` | Server clock | Expiry (default 24h via `JWT_EXPIRATION_MS`) |

**Signing:** HS256 with base64-encoded secret (`JWT_SECRET`).

---

## Request Authentication (JwtAuthenticationFilter)

For each request with `Authorization: Bearer <token>`:

1. Extract username from JWT
2. Load `UserDetails` via `CustomUserDetailsService`
3. `JwtUtil.validateToken()` — checks username, expiry, password-change invalidation
4. Load `UserSession` by `sessionId` claim — must exist and not be revoked
5. If session has `deviceId`, JWT `deviceId` claim must match
6. Set Spring Security context with role + permission authorities

**Invalid/expired tokens:** Filter silently continues without authentication; protected endpoints return 401/403.

---

## Authorization Model

### Roles

| Role | Description |
|------|-------------|
| `STUDENT` | Mark own attendance, view reports |
| `SUBJECT_TEACHER` | Session lifecycle, manual marking, section reports |
| `CLASS_TEACHER` | Same permissions as SUBJECT_TEACHER |
| `HOD` | Department analytics, teacher assignments, all teacher session permissions |
| `ADMIN` | All permissions |

### Permissions

Enforced via `@PreAuthorize("hasAuthority('PERMISSION_NAME')")` on controller methods.

See [role_permission_matrix.md](role_permission_matrix.md) for the full mapping.

### Authority Granting

`CustomUserDetailsService` grants:
- `ROLE_<roleName>` (e.g., `ROLE_ADMIN`)
- Each permission from `role.getPermissions()` (e.g., `CREATE_SESSION`)

`@PreAuthorize("hasRole('ADMIN')")` checks `ROLE_ADMIN`.  
`@PreAuthorize("hasAuthority('MANAGE_USERS')")` checks permission directly.

---

## Password Handling

- **Hashing:** BCrypt via Spring `PasswordEncoder`
- **Storage:** `users.password` column (never returned in API responses)
- **Token hashing:** SHA-256 hex for OTPs and refresh tokens (`TokenHashUtil`)
- **Invalidation:** JWTs issued before `lastPasswordChange` are rejected

---

## Account Security Features

| Feature | Implementation |
|---------|----------------|
| Account lockout | 5 failed logins → 15 min `accountLockedUntil` |
| Session revocation | Login revokes all sessions; logout/refresh rotate tokens |
| Password change invalidation | `JwtUtil` compares `iat` vs `lastPasswordChange` |
| Device binding | JWT `deviceId` must match `UserSession.deviceId` when set |
| Audit logging | Login, session, role, device events in `audit_logs` |

**Not enforced (known gaps):**
- `User.isBlocked` field exists but is not checked during authentication
- No rate limiting on OTP endpoints

---

## Admin Bootstrap

On startup, `AdminBootstrap` creates or repairs an admin user:
- Username: `ADMIN_USERNAME` env (no default in `application.properties`)
- Password: `ADMIN_PASSWORD` env
- Role: `ADMIN`

---

## Public vs Protected Routes

| Public | Protected |
|--------|-----------|
| `POST /auth/*` | `/student/*` |
| `GET /health` | `/sessions/*` |
| `GET /` (no controller) | `/teacher/*`, `/hod/*`, `/admin/*` |
| | `/sections/*`, `/departments/*`, `/greet` |

---

## Related Documentation

- [Role & Permission Matrix](role_permission_matrix.md)
- [Device Registration Workflow](device_registration_workflow.md)
- [Security Considerations](security_considerations.md)

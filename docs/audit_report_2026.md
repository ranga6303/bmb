# Repository Audit Report

**Audit date:** 2026-06-16  
**Repository:** `demo` (Spring Boot attendance API)  
**Source path:** `c:\Users\bangg\OneDrive\Desktop\main\demo`  
**Git remote:** `https://github.com/ranga6303/bmb`

---

## 1. Executive Summary

This is a **Spring Boot 4.0.3 / Java 17** college attendance API with JWT authentication, role-based permissions, teacher session lifecycle management, student attendance marking (BLE beacon + ECDSA signature + device binding), HOD assignment/reporting, and admin governance. A React Native mobile client exists separately in `main\f1`.

**Overall assessment:** The architecture is sound and documentation is above average for a student project, but **security and testing gaps prevent production readiness** without remediation.

**Strengths:**
- Clear controller → service → repository layering
- Permission-based authorization via `@PreAuthorize`
- Refresh-token rotation with revocable `UserSession` records
- Attendance buffer → approval workflow prevents premature finalization
- Existing docs largely match implementation
- Device binding with ECDSA signature verification

**Critical blockers:**
- Hardcoded Resend API key committed in `application.properties`
- Student attendance can be marked **without cryptographic signature** when no public key is registered
- Test suite fails to load Spring context in CI/local runs
- Permissive CORS configuration allowing all origins
- Debug endpoint exposing password hashes

**Overall Repository Score: 5.8 / 10 — Grade C**

---

## 2. Repository Scorecard

| Area           | Score (/10) | Notes |
| -------------- | ----------- | ----- |
| Architecture   | 7.0         | Clean layering; some duplicated report logic between HOD and Department controllers |
| Security       | 4.0         | Secret exposure, attendance bypass, IDOR on department reports, diagnostic endpoint, permissive CORS |
| Performance    | 6.0         | Report endpoints loop with per-section queries; `findAll()` subjects in student report |
| Database       | 7.0         | Good indexes/constraints; `ddl-auto=update` default risky; legacy unused fields remain |
| Backend Design | 6.5         | Solid services; missing ownership checks on lock/cancel; audit actor bug |
| API Design     | 7.0         | Consistent REST patterns; mixed error semantics; no versioning |
| Code Quality   | 6.0         | Debug logging in prod path; dead code; test/code mismatch on login deviceId |
| Testing        | 2.5         | 3 test classes; security tests fail on context load; no integration tests for workflows |
| Documentation  | 7.5         | Strong existing docs; some inaccuracies corrected in this audit cycle |

---

## 3. Critical Findings

### C-1: Hardcoded Resend API Key in Version Control

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Category** | Security — Secret Management |
| **Location** | `src/main/resources/application.properties:23` |
| **Description** | A live Resend API key (`re_K8fRmLq7_...`) is hardcoded in `application.properties`, which is tracked by git. |
| **Impact** | Anyone with repo access can send email via the project's Resend account, incur costs, and impersonate the application. |
| **Root Cause** | Secret placed directly in committed config instead of environment variable. |
| **Recommended Fix** | Rotate the key immediately in Resend dashboard. Change to `resend.api.key=${RESEND_API_KEY}` with no default. Add `application.properties` secrets to `.env` only. Scan git history and purge if pushed to remote. |

**Security verification:**
- **Attacker:** Anyone with repository or deployment artifact access
- **Controlled input:** N/A — key is exposed statically
- **Attack path:** Read `application.properties` → use key with Resend API
- **Evidence:** Line 23 of `application.properties` contains `resend.api.key=re_K8fRmLq7_FxJtFZbGpAwB5Hg4qrXFjnwW`

---

### C-2: Attendance Marking Without Signature Verification

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Category** | Security — Authentication Bypass (Attendance) |
| **Location** | `SessionService.handleDeviceBinding()` lines 474–487 |
| **Description** | When a student has no stored `publicKey` and the request omits `publicKey`, attendance is accepted after only beacon UUID and device ID checks. No ECDSA signature is required. |
| **Impact** | A student (or attacker with stolen JWT) can mark attendance by supplying a known session code and beacon UUID without proving device possession via cryptographic signature. |
| **Root Cause** | `handleDeviceBinding` only enters signature verification branches when `publicKey` is present; no `else` rejection for missing keys. |
| **Recommended Fix** | Reject attendance when `student.publicKey == null` unless `publicKey`, `signedPayload`, and `deviceSignature` are all provided and verified. Make signature fields `@NotBlank` after first registration. |

**Security verification:**
- **Attacker:** Authenticated student without registered public key
- **Controlled input:** `POST /student/attendance` body without `publicKey`, `deviceSignature`, `signedPayload`
- **Attack path:** Obtain active session code + beacon UUID (from `GET /student/active-session`) → POST attendance with matching `beaconUuid` and arbitrary `deviceId` → buffer mark succeeds
- **Evidence:** `handleDeviceBinding` ends without throwing when both `request.getPublicKey()` and `student.getPublicKey()` are null; `markAttendance` proceeds to save `AttendanceBuffer`

---

### C-3: Permissive CORS Configuration

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Category** | Security — Access Control |
| **Location** | `SecurityConfig.corsConfigurationSource()` line 33 |
| **Description** | `allowedOrigins = "*"` allows any origin to make requests to the API. |
| **Impact** | Cross-origin attacks are possible; any website can make requests to the API from browsers. While JWT authentication mitigates this somewhat, it increases attack surface. |
| **Root Cause** | Development-friendly CORS configuration left in place. |
| **Recommended Fix** | Restrict to known mobile/web client origins in production configuration. |

**Security verification:**
- **Attacker:** Any website operator
- **Controlled input:** Origin header in HTTP request
- **Attack path:** Malicious website makes requests to API using user's stored JWT (if leaked via XSS)
- **Evidence:** Line 33 of `SecurityConfig.java`: `configuration.setAllowedOrigins(List.of("*"));`

---

## 4. High Findings

### H-1: Login Ignores `deviceId` from Request

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Correctness / Security |
| **Location** | `AuthService.loginInternal()` lines 255–277 |
| **Description** | `LoginRequest.deviceId` is never read. JWT and `UserSession` use `user.getRegisteredDeviceId()` instead. |
| **Impact** | Device binding at login does not work as documented/tested. JWT `deviceId` claim is null until attendance marks bind a device. Device-ID enforcement in `JwtAuthenticationFilter` is ineffective for new sessions. |
| **Root Cause** | Implementation drift — `deviceId` added to DTO but not wired in service. |
| **Recommended Fix** | Use `request.getDeviceId()` when creating `UserSession` and JWT claims. Update `registeredDeviceId` on user only via attendance/device-change flows, not login. |

**Evidence:** `grep getDeviceId` in `src/` shows usage only in `MarkAttendanceRequest`, `SessionService`, tests — not in `AuthService`.

---

### H-2: Department Report IDOR

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Security — Broken Access Control |
| **Location** | `DepartmentController.report()` line 52 |
| **Description** | `GET /departments/{name}/report` accepts any department name path variable with no scoping to the requesting HOD's department. |
| **Impact** | Any user with `VIEW_DEPARTMENT_ANALYTICS` (HOD, ADMIN) can read attendance analytics for all departments. |
| **Root Cause** | Missing authorization check tying `{name}` to caller's department. |
| **Recommended Fix** | Resolve caller's department from `TeacherSectionSubject` mappings (as `HodController` does) and reject mismatched `{name}`. |

**Security verification:**
- **Attacker:** HOD user authenticated with `VIEW_DEPARTMENT_ANALYTICS`
- **Controlled input:** Path variable `{name}` = another department's exact name
- **Attack path:** `GET /departments/OtherDept/report` with valid HOD JWT
- **Evidence:** `DepartmentController` has no call to `ensureSectionBelongsToCurrentHodDepartment` or equivalent

---

### H-3: Session Lock/Cancel Missing Teacher Ownership

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Security — Broken Access Control |
| **Location** | `SessionService.lockSession()` lines 227–238; `cancelSession()` lines 277–289 |
| **Description** | `approveSession` and `manualMark` verify the acting teacher owns the session, but `lockSession` and `cancelSession` do not. |
| **Impact** | Any user with `LOCK_SESSION` or `CANCEL_SESSION` (including HOD role) can lock or cancel another teacher's session by ID. |
| **Root Cause** | Inconsistent authorization within session lifecycle methods. |
| **Recommended Fix** | Add teacher ownership check matching `approveSession` pattern. |

---

### H-4: Admin Diagnostic Endpoint Exposes Credentials

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Security — Information Disclosure |
| **Location** | `TestController.testPassword()` lines 20–37 |
| **Description** | `GET /test-password` returns plaintext password, hash, and newly generated hash in JSON response. |
| **Impact** | Compromised admin token enables password/hash exfiltration; endpoint should not exist in production. |
| **Root Cause** | Debug endpoint left active. |
| **Recommended Fix** | Remove endpoint or gate behind `spring.profiles.active=dev` profile only. |

---

### H-5: Internal Error Messages Leaked to Clients

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Security — Information Disclosure |
| **Location** | `GlobalExceptionHandler.handleRuntime()` line 72 |
| **Description** | Unhandled `RuntimeException` responses include `ex.getMessage()` in the HTTP body. |
| **Impact** | Stack-related or SQL error details may reach API consumers. |
| **Root Cause** | Generic runtime handler returns raw exception message. |
| **Recommended Fix** | Return generic "Internal server error" to clients; log full exception server-side only. |

---

### H-6: `User.isBlocked` Not Enforced at Authentication

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Security — Authorization |
| **Location** | `CustomUserDetailsService.loadUserByUsername()` lines 36–45 |
| **Description** | `blocked` field exists on `User` entity but is never checked during authentication or JWT validation. |
| **Impact** | Blocked users (if set via direct DB manipulation or future feature) can still authenticate. |
| **Root Cause** | Blocking feature partially removed (admin endpoints commented out) but entity field remains unused. |
| **Recommended Fix** | Check `!user.isBlocked()` in `CustomUserDetailsService` or reject in `AuthService.loginInternal`. |

---

### H-7: Device Change Request Allows Unbound Users

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Correctness |
| **Location** | `DeviceChangeService.submitRequest()` lines 40–62 |
| **Description** | Documentation states device change requires existing `registeredDeviceId`, but code accepts requests when `currentDeviceId` is null. |
| **Impact** | Students without device binding can submit meaningless change requests. |
| **Root Cause** | Missing validation guard. |
| **Recommended Fix** | Throw `CustomException` when `actor.getRegisteredDeviceId()` is null or blank. |

---

### H-8: Test Suite Fails to Load Application Context

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Category** | Testing / Maintainability |
| **Location** | `application.properties` lines 27–29; `application-test.properties` |
| **Description** | `app.admin.username=${ADMIN_USERNAME}` has no default. Test profile does not define `ADMIN_USERNAME`, causing `DeviceIdSecurityTest` and related `@SpringBootTest` classes to fail. |
| **Impact** | CI cannot validate security regressions; false confidence in test coverage. |
| **Root Cause** | Required env placeholders added to main properties without test overrides. |
| **Recommended Fix** | Add admin bootstrap defaults in `application-test.properties` or use `${ADMIN_USERNAME:admin}` in main config. |

**Evidence:** `target/surefire-reports/...DeviceIdSecurityTest.txt` — `PlaceholderResolutionException: Could not resolve placeholder 'ADMIN_USERNAME'`

---

## 5. Medium Findings

### M-1: Debug Logging in Signature Verification

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Category** | Security / Code Quality |
| **Location** | `DeviceVerificationService.verifySignatureWithKey()` lines 37–42 |
| **Description** | `System.out.println` logs signed payloads and key fragments to stdout. |
| **Impact** | Sensitive cryptographic material in production logs. |
| **Recommended Fix** | Remove debug prints or use trace-level SLF4J behind debug flag. |

---

### M-2: Default Admin Bootstrap Password

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Category** | Security |
| **Location** | `AdminBootstrap.java` line 22 |
| **Description** | Fallback default password `Admin@123` if env not set. |
| **Impact** | Predictable admin credentials in misconfigured deployments. |
| **Recommended Fix** | Fail startup if `ADMIN_PASSWORD` not set in non-dev profiles. |

---

### M-3: Hibernate `ddl-auto=update` Default

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Category** | Database / Operations |
| **Location** | `application.properties` line 18 |
| **Description** | Schema auto-mutation enabled by default. |
| **Impact** | Unreviewed schema changes in production; data loss risk on entity changes. |
| **Recommended Fix** | Default to `validate` in production; use Flyway/Liquibase migrations. |

---

### M-4: Audit Log Records Wrong Actor on Role Assignment

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Category** | Correctness |
| **Location** | `AuthService.assignRole()` line 373 |
| **Description** | `persistAudit("ASSIGN_ROLE", user, ...)` passes target user as actor instead of admin. |
| **Impact** | Audit trail incorrectly attributes role changes. |
| **Recommended Fix** | Accept `User adminActor` parameter from `AdminController`. |

---

### M-5: Inefficient Student Attendance Report Query

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Category** | Performance |
| **Location** | `SessionService.getOwnAttendanceReport()` line 368 |
| **Description** | `subjectRepository.findAll()` loads every subject in the database per request. |
| **Impact** | Unnecessary memory/DB load as subject catalog grows. |
| **Recommended Fix** | Query only subjects mapped to the student's section. |

---

### M-6: Documentation Inaccuracies (Corrected)

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Category** | Documentation |
| **Location** | `docs/api_reference.md` lines 103–107 |
| **Description** | Docs claim device-change approval requires non-empty `reason` and rejects conflicting `newDeviceId`; `DeviceChangeService.approveRequest` implements neither check. |
| **Recommended Fix** | Align code with docs or update docs to match code (see updated `device_registration_workflow.md`). |

---

## 6. Low Findings

| ID | Severity | Category | Location | Description |
|----|----------|----------|----------|-------------|
| L-1 | Low | Maintainability | `SessionService.ensureDeviceChangeRequestExists()` | Private method never called — dead code |
| L-2 | Low | Maintainability | `User.blocked`, `blockReason`, `blockedAt` | Legacy fields; admin unblock endpoints commented out |
| L-3 | Low | Operations | `seed-data.ps1` | Calls inactive `POST /admin/users` endpoint |
| L-4 | Low | Operations | Repository root | No Dockerfile, docker-compose, or CI workflow |
| L-5 | Low | API Design | Various controllers | Success responses mix `MessageResponse` strings and raw maps without envelope consistency |

**No issues found** for: SQL injection (parameterized JPA queries), command injection, SSRF, or direct file-access vulnerabilities.

---

## 7. Improvement Recommendations

### Immediate (before any production deployment)
1. Rotate and externalize the Resend API key
2. Require ECDSA signature on all student attendance marks
3. Fix test configuration so security tests pass
4. Remove or profile-gate `/test-password`
5. Add teacher ownership checks to lock/cancel session
6. Restrict CORS origins to specific domains

### Short-term (next sprint)
7. Wire `LoginRequest.deviceId` into session/JWT creation
8. Scope department reports to caller's department
9. Enforce `registeredDeviceId` prerequisite on device-change requests
10. Stop returning raw exception messages to API clients
11. Remove debug `System.out.println` from `DeviceVerificationService`
12. Enforce `User.isBlocked` check in authentication flow

### Medium-term
13. Add Flyway migrations; set `ddl-auto=validate` in production
14. Add integration tests for registration, session lifecycle, and attendance flows
15. Add Docker + CI pipeline (build, test, security scan)
16. Implement master-data admin APIs or document SQL seeding as the supported path
17. Fix audit logging to record correct actor
18. Optimize report queries to avoid loading unnecessary data

---

## 8. Updated Documentation Summary

The following docs were reviewed during this audit:

| Document | Status |
|----------|--------|
| `audit_report.md` | **Updated** — this file (new audit dated 2026-06-16) |
| `architecture_overview.md` | Reviewed — accurate |
| `authentication_authorization.md` | Reviewed — accurate |
| `database_design.md` | Reviewed — accurate |
| `attendance_workflow.md` | Reviewed — accurate |
| `device_registration_workflow.md` | Reviewed — accurate |
| `api_reference.md` | Reviewed — minor inaccuracies noted |
| `codebase_overview.md` | Reviewed — accurate |
| `project_explanation.md` | Reviewed — accurate |
| `system_flow.md` | Reviewed — accurate |

---

## 9. Conclusion

This repository represents a well-structured Spring Boot application with good architectural patterns and comprehensive documentation. However, several critical security vulnerabilities must be addressed before production deployment. The most severe issues are:

1. Hardcoded API credentials in version control
2. Attendance marking bypass when no public key is registered
3. Permissive CORS configuration
4. Missing authorization checks in several endpoints

The testing infrastructure needs attention to ensure security regressions are caught. Once the critical and high-severity issues are resolved, this codebase will be suitable for production deployment with appropriate monitoring and operational practices.

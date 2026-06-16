# Attendance Workflow

## Overview

Attendance follows a **buffer-then-approve** model. Student and teacher marks are stored provisionally in `AttendanceBuffer` while a session is `ACTIVE`. Final `Attendance` records (PRESENT/ABSENT) are created only when the owning teacher approves a `LOCKED` session.

---

## Session State Machine

```text
                    ┌─────────────┐
                    │   ACTIVE    │◄── Teacher creates session
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │ Manual lock   │ Auto-expiry   │
           │ POST /lock    │ (scheduler)   │
           ▼               ▼               │
    ┌─────────────┐  ┌─────────────┐       │
    │   LOCKED    │  │   LOCKED    │       │
    └──────┬──────┘  └──────┬──────┘       │
           │                │               │
     Approve│          Cancel│         Auto-cancel
     POST   │          POST   │         (1hr locked)
           ▼                ▼               ▼
    ┌─────────────┐  ┌─────────────┐ ┌─────────────┐
    │  APPROVED   │  │  CANCELLED  │ │  CANCELLED  │
    │ (final rows)│  │ (no records)│ │ (no records)│
    └─────────────┘  └─────────────┘ └─────────────┘
```

---

## Phase 1: Session Creation

**Endpoint:** `POST /sessions`  
**Permission:** `CREATE_SESSION`  
**Actor:** Mapped teacher

### Preconditions
- Teacher profile linked to authenticated user
- Teacher mapped to requested subject AND section (`TeacherSectionSubject`)
- Subject mapped to section
- No existing ACTIVE or LOCKED session for this teacher
- No existing ACTIVE or LOCKED session for this section
- Room exists (by `roomNumber` or `beaconUuid`)
- Room has beacon UUID, positive dimensions, and safe radius
- No ACTIVE or LOCKED session in that room

### Result
- Session saved with status `ACTIVE`
- Unique 6-digit `sessionCode` generated
- `expiryTime` = now + 60 minutes
- `SESSION_CREATED` audit log

> **Inactive rules (commented out in code):** Sunday blocking, 07:00–23:00 window, one session per subject/section/day.

---

## Phase 2: Student Auto-Mark

**Endpoint:** `POST /student/attendance`  
**Permission:** `VIEW_OWN_ATTENDANCE`

### Request Body

| Field | Required | Validation |
|-------|----------|------------|
| `sessionCode` | Yes | Exactly 6 digits |
| `beaconUuid` | Yes | Must match session room |
| `deviceId` | Yes | Must match registered device (if bound) |
| `publicKey` | First mark only | Base64 X.509 EC key |
| `signedPayload` | When signing | Format: `sessionCode:beaconUuid:deviceId:nonce` |
| `deviceSignature` | When signing | Base64 ECDSA signature |

### Validation Chain

1. Find ACTIVE session by `sessionCode`
2. Session not expired
3. Student profile exists for authenticated user
4. Student's section matches session section
5. `beaconUuid` matches session room beacon
6. **Device binding** (`handleDeviceBinding`):
   - First mark: reject if `deviceId` already registered to another user
   - Returning: `deviceId` must match `user.registeredDeviceId`
   - First mark with `publicKey`: validate key format, verify ECDSA signature, save key + device
   - Returning with stored key: verify signature against stored public key
7. Reject duplicate buffer entry for same session/student

### Result
- `AttendanceBuffer` saved with `MarkType.AUTO`

> **Security gap:** If student has no `publicKey` and request omits `publicKey`, steps 6 signature checks are skipped and attendance is still accepted. See [security_considerations.md](security_considerations.md).

---

## Phase 3: Teacher Manual Mark

**Endpoint:** `POST /sessions/{id}/manual?studentId=`  
**Permission:** `MANUAL_MARK_ATTENDANCE`

### Preconditions
- Session is `ACTIVE` (not APPROVED/CANCELLED)
- Authenticated teacher owns the session
- Target student exists and belongs to session section
- No existing buffer mark for that student

### Result
- `AttendanceBuffer` saved with `MarkType.MANUAL`
- `MANUAL_ATTENDANCE` audit log

---

## Phase 4: Lock Session

**Endpoint:** `POST /sessions/{id}/lock`  
**Permission:** `LOCK_SESSION`

- Session must be `ACTIVE`
- Status → `LOCKED`, `lockedAt` = now
- No new student marks accepted (session no longer ACTIVE)

> **Known gap:** No verification that acting teacher owns the session. Any user with `LOCK_SESSION` (including HOD) can lock any session by ID.

**Auto-lock:** Scheduler runs every 60s, locks ACTIVE sessions past `expiryTime`.

---

## Phase 5: Approve Session

**Endpoint:** `POST /sessions/{id}/approve`  
**Permission:** `APPROVE_SESSION`

### Preconditions
- Session is `LOCKED`
- Authenticated teacher **owns** the session

### Finalization Logic
1. Load all `AttendanceBuffer` rows for session
2. Load all students in session section
3. For each student:
   - In buffer → `Attendance` with status `PRESENT`
   - Not in buffer → `Attendance` with status `ABSENT`
4. Session status → `APPROVED`, `approvedAt` = now
5. Delete all buffer rows for session
6. `SESSION_APPROVED` audit log

---

## Phase 6: Cancel Session

**Endpoint:** `POST /sessions/{id}/cancel`  
**Permission:** `CANCEL_SESSION`

- Session must be `LOCKED`
- Delete buffer rows
- Status → `CANCELLED`, `cancelledAt` = now
- No final attendance records created

**Auto-cancel:** Scheduler runs every 300s, cancels LOCKED sessions older than 1 hour.

---

## Active Session Discovery

### Student: `GET /student/active-session`
- Finds ACTIVE session in student's section
- Returns session code, beacon UUID, room, teacher, expiry
- Rejects if no active session or expired

### Teacher: `GET /sessions/active`
- Finds teacher's ACTIVE or LOCKED session
- Returns 204 if none

---

## Reporting (Post-Approval Only)

Reports count only `APPROVED` sessions and `PRESENT` attendance status.

| Report | Endpoint | Permission |
|--------|----------|------------|
| Own attendance | `GET /student/my-attendance` | `VIEW_OWN_ATTENDANCE` |
| Section/subject | `GET /sections/{id}/attendance?subjectId=` | `VIEW_SECTION_ATTENDANCE` |
| Department | `GET /departments/{name}/report` | `VIEW_DEPARTMENT_ANALYTICS` |
| HOD department | `GET /hod/department/report` | `VIEW_DEPARTMENT_ANALYTICS` or `ASSIGN_TEACHER_SECTION` |

---

## Sequence Diagram

```text
Teacher                API                    Student
  │                     │                        │
  │── POST /sessions ──►│                        │
  │◄── sessionId ───────│                        │
  │                     │◄── POST /attendance ───│
  │                     │── buffer saved ────────►│
  │── POST /lock ──────►│                        │
  │                     │   (no more marks)      │
  │── POST /approve ───►│                        │
  │                     │── final Attendance ───►│
  │◄── approved ────────│                        │
```

---

## Related Documentation

- [Device Registration Workflow](device_registration_workflow.md)
- [Authentication & Authorization](authentication_authorization.md)
- [System Flow (detailed sequences)](system_flow.md)

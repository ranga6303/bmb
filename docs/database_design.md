# Database Design

## Overview

MySQL database managed by Hibernate/JPA. Schema is auto-generated via `spring.jpa.hibernate.ddl-auto` (default: `update`). Tests use H2 in-memory with `create-drop`.

No migration tool (Flyway/Liquibase) is configured.

---

## Entity Relationship Diagram

```text
┌──────────┐     1:1      ┌──────────┐
│  User    │◄────────────►│ Student  │
└────┬─────┘              └────┬─────┘
     │ 1:1                     │ N:1
     ▼                         ▼
┌──────────┐              ┌──────────┐
│ Teacher  │              │ Section  │
└────┬─────┘              └────┬─────┘
     │                           │
     │ M:N (mapped_subjects)     │ M:N (mapped_sections)
     ▼                           ▼
┌──────────┐◄─────────────────►┌──────────┐
│ Subject  │                  │ Subject  │
└──────────┘                  └──────────┘

┌──────────┐     N:1      ┌──────────┐
│ Session  │─────────────►│ Teacher  │
│          │─────────────►│ Subject  │
│          │─────────────►│ Section  │
│          │─────────────►│ Room     │
└────┬─────┘              └──────────┘
     │ 1:N
     ├──────────────────► AttendanceBuffer (provisional)
     └──────────────────► Attendance (final, post-approval)

┌──────────────┐  N:1  ┌──────────┐
│ UserSession  │──────►│ User     │
└──────────────┘       └──────────┘

┌─────────────────────┐  N:1  ┌──────────┐
│ DeviceChangeRequest │──────►│ User     │
└─────────────────────┘       └──────────┘

┌──────────────────────────┐
│ TeacherSectionSubject    │  (HOD assignment junction)
│ teacher + section + subject │
└──────────────────────────┘
```

---

## Core Tables

### `users`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, auto-increment | |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | College ID |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | |
| `password` | VARCHAR(100) | NOT NULL | BCrypt hash |
| `role` | VARCHAR | NOT NULL | Enum: STUDENT, SUBJECT_TEACHER, etc. |
| `enabled` | BOOLEAN | NOT NULL | Default true |
| `email_verified` | BOOLEAN | NOT NULL | |
| `created_at` | TIMESTAMP | NOT NULL | |
| `last_password_change` | TIMESTAMP | | Invalidates old JWTs |
| `registered_device_id` | VARCHAR(255) | INDEX | Mobile device binding |
| `is_blocked` | BOOLEAN | NOT NULL | **Legacy — not enforced in auth** |
| `block_reason` | VARCHAR | | Legacy |
| `blocked_at` | TIMESTAMP | | Legacy |
| `failed_login_attempts` | INT | NOT NULL | Lockout counter |
| `account_locked_until` | TIMESTAMP | | Temporary lock |
| `last_login_at` | TIMESTAMP | | |

**Indexes:** `idx_users_email`, `idx_users_username`, `idx_users_registered_device_id`

---

### `students`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK | |
| `user_id` | BIGINT | UNIQUE, FK → users | One-to-one |
| `student_id` | VARCHAR(50) | UNIQUE, NOT NULL | College ID |
| `name` | VARCHAR(100) | NOT NULL | |
| `email` | VARCHAR(255) | | Preloaded from admin |
| `section_id` | BIGINT | FK → sections, NOT NULL | |
| `public_key` | TEXT | | Base64 X.509 EC public key |

---

### `teachers`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK | |
| `user_id` | BIGINT | UNIQUE, FK → users | Nullable until registration |
| `teacher_id` | VARCHAR(50) | UNIQUE, NOT NULL | College ID |
| `name` | VARCHAR(100) | NOT NULL | |
| `email` | VARCHAR(255) | | |

**Many-to-many:** `teacher_mapped_subjects`, `teacher_mapped_sections` (join tables)

---

### `sections`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK |
| `name` | VARCHAR | NOT NULL |
| `department_name` | VARCHAR | NOT NULL |

---

### `subjects`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK |
| `name` | VARCHAR | UNIQUE, NOT NULL |

**Many-to-many:** `subject_mapped_sections` (join table)

---

### `rooms`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `room_number` | VARCHAR | PK | |
| `beacon_uuid` | VARCHAR | UNIQUE | BLE identifier |
| `length` | DOUBLE | | Room dimension (meters) |
| `width` | DOUBLE | | |
| `safe_radius` | DOUBLE | | Proximity threshold |

---

### `sessions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK | |
| `version` | BIGINT | | Optimistic locking |
| `teacher_id` | BIGINT | FK, NOT NULL | |
| `subject_id` | BIGINT | FK, NOT NULL | |
| `section_id` | BIGINT | FK, NOT NULL | |
| `room_number` | VARCHAR | FK → rooms, NOT NULL | |
| `session_code` | VARCHAR(6) | UNIQUE, NOT NULL | 6-digit code |
| `status` | VARCHAR | NOT NULL | ACTIVE, LOCKED, APPROVED, CANCELLED |
| `session_date` | DATE | | |
| `start_time` | TIMESTAMP | | |
| `expiry_time` | TIMESTAMP | | Default +60 min |
| `locked_at` | TIMESTAMP | | |
| `approved_at` | TIMESTAMP | | |
| `cancelled_at` | TIMESTAMP | | |

**Indexes:** `idx_sessions_status`, `idx_sessions_session_code`, `idx_sessions_section_status`

---

### `attendance_buffer` (provisional marks)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK |
| `session_id` | BIGINT | FK, NOT NULL |
| `student_id` | BIGINT | FK, NOT NULL |
| `mark_type` | VARCHAR | AUTO or MANUAL |
| `marked_at` | TIMESTAMP | |

Unique constraint on (session_id, student_id) prevents duplicate marks.

---

### `attendance` (final records)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK |
| `session_id` | BIGINT | FK, NOT NULL |
| `student_id` | BIGINT | FK, NOT NULL |
| `status` | VARCHAR | PRESENT or ABSENT |

Created only on session approval.

---

### `user_sessions` (refresh tokens)

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | FK, NOT NULL |
| `refresh_token_hash` | VARCHAR | SHA-256 hex |
| `device_id` | VARCHAR | From user at login |
| `ip_address` | VARCHAR | |
| `user_agent` | VARCHAR | |
| `last_active_at` | TIMESTAMP | |
| `revoked` | BOOLEAN | |

---

### `device_change_requests`

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK |
| `user_id` | BIGINT | FK, NOT NULL |
| `old_device_id` | VARCHAR | |
| `new_device_id` | VARCHAR | NOT NULL |
| `reason` | VARCHAR | Optional |
| `status` | VARCHAR | PENDING, APPROVED, REJECTED |
| `requested_at` | TIMESTAMP | |
| `resolved_at` | TIMESTAMP | |
| `resolved_by` | BIGINT | FK → users |
| `admin_remarks` | VARCHAR | |

---

### Token Tables

| Table | Purpose | Key fields |
|-------|---------|------------|
| `email_verification_tokens` | Registration OTP | `token_hash`, `college_id`, `expiry_time`, `used` |
| `password_reset_tokens` | Password reset OTP | `token_hash`, `user_id`, `expiry_time`, `used` |

---

### `audit_logs`

| Column | Type | Notes |
|--------|------|-------|
| `action` | VARCHAR | e.g., LOGIN_SUCCESS, SESSION_CREATED |
| `actor_user_id` | BIGINT | FK → users |
| `target_entity` | VARCHAR | e.g., Session, User |
| `target_id` | BIGINT | |
| `details` | VARCHAR | Free text |
| `timestamp` | TIMESTAMP | Auto-set |

---

### `teacher_section_subjects`

Junction table for HOD-managed teaching assignments.

| Column | Type |
|--------|------|
| `teacher_id` | FK |
| `section_id` | FK |
| `subject_id` | FK |

Unique on (teacher_id, section_id, subject_id).

---

## Data Integrity

**Enforced in DB:**
- Unique usernames, emails, session codes, student/teacher IDs
- Foreign keys via JPA relationships
- Optimistic locking on `sessions.version`

**Enforced in application only:**
- One active/locked session per teacher and per section
- One active/locked session per room
- Teacher must be mapped to subject+section before creating session
- Session approval creates exactly one attendance row per section student

---

## Query Patterns

| Pattern | Location | Notes |
|---------|----------|-------|
| Aggregated attendance counts | `AttendanceRepository` JPQL | `countPresentByStudent`, `countPresentBySubjectForStudent` |
| Session status filters | `SessionRepository` | `findByStatusAndExpiryTimeBefore`, etc. |
| Teacher mappings | `TeacherRepository.findAllWithMappings` | Eager fetch for HOD/admin lists |

**Performance concern:** Report endpoints in `DepartmentController` and `HodController` loop sections/subjects with individual count queries per iteration.

---

## Configuration

```properties
# Production (from application.properties)
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
spring.jpa.open-in-view=false

# Tests (application-test.properties)
spring.jpa.hibernate.ddl-auto=create-drop
```

**Recommendation:** Use `validate` in production with explicit migrations.

---

## Master Data

No active admin CRUD API exists for creating students, teachers, sections, subjects, or rooms. Master data must be:
- Inserted directly into MySQL, or
- Seeded via `seed-data.ps1` (partially outdated)

Preloaded `Student`/`Teacher` rows must exist before user self-registration.

---

## Related Documentation

- [Architecture Overview](architecture_overview.md)
- [Attendance Workflow](attendance_workflow.md)
- [Deployment Guide](deployment_guide.md)

# AI Context - Attendance Backend (`demo`)

Current snapshot of the backend under `src/main/java/com/example/demo`.

## Project Summary

Spring Boot API for a role-based college attendance system. The current code supports:

- OTP-based registration from pre-existing student/teacher records
- JWT access + refresh-token session model
- Session lifecycle (create, lock, approve, cancel, manual mark)
- Device-bound and signature-verified student attendance marking
- HOD workflows for teacher-section-subject assignment and class teacher assignment
- Admin workflows for role assignment, subject management, and device-change approvals
- Section and department attendance reports

## Tech Stack

- Java 17
- Spring Boot 4.0.3
- Spring Security + method-level authorization
- Spring Data JPA / Hibernate
- MySQL runtime, H2 tests
- JWT (`jjwt` 0.12.7)
- Spring Mail, Scheduling
- `dotenv-java`

## Security Baseline

`SecurityConfig` permits:

- `/auth/**`
- `/`
- `/health`

Everything else requires authentication, with `@PreAuthorize` checks on controllers.

## Active Controller Surface

- `auth/AuthController` -> `/auth/*` registration, login, refresh, reset, logout
- `controller/StudentController` -> `/student/*` student profile/session/attendance/device-change
- `controller/SessionController` -> `/sessions/*` teacher session lifecycle and buffers
- `controller/TeacherController` -> `/teacher/*` departments/sections/subjects/assignments
- `controller/HodController` -> `/hod/*` teacher list, department report, assignment ops
- `controller/AdminController` -> `/admin/*` roles, subject assignment, device-change moderation
- `controller/SectionController` -> `/sections/{id}/attendance`
- `controller/DepartmentController` -> `/departments/{name}/report`
- `controller/HealthC` -> `/health`
- `auth/GreetingController` -> `/greet`
- `controller/TestController` -> `/test-password` (admin diagnostic)

## Role/Permission Shape

- `STUDENT`: `VIEW_OWN_ATTENDANCE`
- `SUBJECT_TEACHER`: session lifecycle + section report permissions
- `CLASS_TEACHER`: same operational permissions as subject teacher
- `HOD`: department analytics + section report + assignment permissions
- `ADMIN`: all permissions in `Permission`

## Key Current Behaviors

- Login revokes existing active sessions for the user and issues fresh access/refresh tokens.
- JWT/session checks enforce session existence and revocation status, with device ID checks when bound.
- Student attendance marking uses signature verification (ECDSA) and first-time key/device binding rules.
- Session approval finalizes attendance: buffered marks become `PRESENT`, remaining section students become `ABSENT`.
- HOD assignment model is now **teacher + section + subject** via `TeacherSectionSubject`.
- Admin now actively exposes subject assignment endpoints for teachers and sections.

## Notable Live Endpoints To Remember

- HOD:
  - `GET /hod/department/report`
  - `POST /hod/teachers/{teacherId}/assign-section-subject?sectionId=&subjectId=`
  - `POST /hod/teachers/{teacherId}/remove-section-subject?sectionId=&subjectId=`
- Teacher:
  - `GET /teacher/assignments`
- Admin:
  - `GET /admin/sections`
  - `GET /admin/subjects`
  - `POST /admin/teachers/{teacherId}/assign-subject?subjectId=`
  - `POST /admin/teachers/{teacherId}/remove-subject?subjectId=`
  - `POST /admin/sections/{sectionId}/assign-subject?subjectId=`
  - `POST /admin/sections/{sectionId}/remove-subject?subjectId=`

## Drift/Caution Notes

- `/` is permitted by security but has no controller mapping.
- Android-ID block/unblock APIs are currently commented out in `AdminController`.
- `TestController` remains active and returns password/hash diagnostic data to admins.

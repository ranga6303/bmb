# API Reference - Attendance Backend (`demo`)

Current API surface from active controller mappings.

## Conventions

- Base path: `/`
- Public routes by security config: `/auth/**`, `/`, `/health`
- Other routes require bearer token: `Authorization: Bearer <accessToken>`
- Most success responses use `{ "message": "..." }` or JSON domain payloads
- Common business errors use `CustomException` -> HTTP 400

## Auth (`/auth`)

- `POST /auth/initiate-registration`
- `POST /auth/complete-registration`
- `POST /auth/login`
- `POST /auth/login/student`
- `POST /auth/login/staff`
- `POST /auth/forgot-password`
- `POST /auth/refresh`
- `POST /auth/reset-password`
- `POST /auth/logout`

## Student (`/student`)

Requires `VIEW_OWN_ATTENDANCE`.

- `GET /student/me`
- `GET /student/active-session`
- `GET /student/my-attendance`
- `POST /student/attendance`
- `POST /student/device-change-request`
- `GET /student/device-change-requests`

## Sessions (`/sessions`)

- `POST /sessions` (`CREATE_SESSION`)
- `GET /sessions/active` (`CREATE_SESSION`)
- `POST /sessions/{id}/lock` (`LOCK_SESSION`)
- `POST /sessions/{id}/approve` (`APPROVE_SESSION`)
- `POST /sessions/{id}/cancel` (`CANCEL_SESSION`)
- `POST /sessions/{id}/manual?studentId=` (`MANUAL_MARK_ATTENDANCE`)
- `GET /sessions/{id}/buffer` (`CREATE_SESSION`)

## Teacher (`/teacher`)

- `GET /teacher/departments`
  - Auth: `CREATE_SESSION` or `ASSIGN_TEACHER_SECTION` or `VIEW_SECTION_ATTENDANCE`
- `GET /teacher/sections?department=`
  - Auth: `CREATE_SESSION` or `ASSIGN_TEACHER_SECTION` or `VIEW_SECTION_ATTENDANCE`
- `GET /teacher/subjects`
  - Auth: `CREATE_SESSION` or `ASSIGN_TEACHER_SECTION`
- `GET /teacher/assignments`
  - Auth: `CREATE_SESSION`

`/teacher/assignments` returns teacher-specific section-subject assignments from `TeacherSectionSubject`.

## HOD (`/hod`)

- `GET /hod/teachers` (`ASSIGN_TEACHER_SECTION`)
- `GET /hod/department/report`
  - Auth: `VIEW_DEPARTMENT_ANALYTICS` or `ASSIGN_TEACHER_SECTION`
- `POST /hod/teachers/{teacherId}/assign-section-subject?sectionId=&subjectId=`
  - Auth: `ASSIGN_TEACHER_SECTION`
- `POST /hod/teachers/{teacherId}/remove-section-subject?sectionId=&subjectId=`
  - Auth: `ASSIGN_TEACHER_SECTION`
- `POST /hod/sections/{sectionId}/assign-class-teacher`
  - Auth: `ASSIGN_CLASS_TEACHER`
  - Body: `{ "teacherId": "T001" }`

## Admin (`/admin`)

All active routes require `MANAGE_USERS`.

- `POST /admin/users/{userId}/role`
- `POST /admin/reset-device/{userId}`
- `GET /admin/teachers`
- `GET /admin/sections`
- `GET /admin/subjects`
- `POST /admin/teachers/{teacherId}/assign-subject?subjectId=`
- `POST /admin/teachers/{teacherId}/remove-subject?subjectId=`
- `POST /admin/sections/{sectionId}/assign-subject?subjectId=`
- `POST /admin/sections/{sectionId}/remove-subject?subjectId=`
- `GET /admin/device-change-requests`
- `POST /admin/device-change-requests/{id}/approve`
- `POST /admin/device-change-requests/{id}/reject`

## Reports

- `GET /sections/{id}/attendance?subjectId=` (`VIEW_SECTION_ATTENDANCE`)
- `GET /departments/{name}/report` (`VIEW_DEPARTMENT_ANALYTICS`)

## Utility

- `GET /health` (public, plain text)
- `GET /greet` (authenticated)
- `GET /test-password` (`ROLE_ADMIN`, diagnostic)

## Disabled/Commented Endpoints

Currently commented out in `AdminController`:

- `GET /admin/blocked-students`
- `POST /admin/unblock/{userId}`

# API Reference - Attendance System API

Base path: /

> Note: All non-/auth/** endpoints are authenticated by the application security configuration. /auth/** is permitted without authentication.

## AuthController (`/auth`)

### POST /auth/initiate-registration

- Auth required: none
- Request body:
  - `collegeId` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /auth/complete-registration

- Auth required: none
- Request body:
  - `collegeId` (string)
  - `otp` (string)
  - `newPassword` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /auth/login

- Auth required: none
- Request body:
  - `username` (string)
  - `password` (string)
  - `deviceId` (string)
- Success response: `AuthResponse`
  - `accessToken` (string)
  - `refreshToken` (string)
  - `tokenType` (string)
- Status codes: `200`

### POST /auth/login/student

- Auth required: none
- Request body: same as `POST /auth/login`
- Success response: `AuthResponse`
- Status codes: `200`

### POST /auth/login/staff

- Auth required: none
- Request body: same as `POST /auth/login`
- Success response: `AuthResponse`
- Status codes: `200`

### POST /auth/forgot-password

- Auth required: none
- Request body:
  - `collegeId` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /auth/refresh

- Auth required: none
- Request body:
  - `refreshToken` (string)
- Success response: `AuthResponse`
- Status codes: `200`

### POST /auth/reset-password

- Auth required: none
- Request body:
  - `collegeId` (string)
  - `otp` (string)
  - `newPassword` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /auth/logout

- Auth required: none
- Request body:
  - `refreshToken` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

## StudentController (`/student`)

### GET /student/me

- Auth required: `VIEW_OWN_ATTENDANCE`
- Success response:
  - `id` (Long)
  - `username` (string)
  - `email` (string)
  - `role` (string)
- Status codes: `200`

### GET /student/active-session

- Auth required: `VIEW_OWN_ATTENDANCE`
- Success response: `ActiveSessionResponse`
  - `sessionId` (Long)
  - `sessionCode` (string)
  - `subjectName` (string)
  - `teacherName` (string)
  - `roomNumber` (string)
  - `beaconUuid` (string)
  - `expiryTime` (string)
- Status codes: `200`

### GET /student/my-attendance

- Auth required: `VIEW_OWN_ATTENDANCE`
- Success response: `StudentOwnAttendanceReport`
  - `studentId` (string)
  - `studentName` (string)
  - `sectionName` (string)
  - `subjects` (array of objects)
    - `subjectId` (Long)
    - `subjectName` (string)
    - `attended` (long)
    - `total` (long)
    - `percentage` (double)
- Status codes: `200`

### POST /student/attendance

- Auth required: `VIEW_OWN_ATTENDANCE`
- Request body:
  - `sessionCode` (string)
  - `beaconUuid` (string)
  - `deviceId` (string)
  - `deviceSignature` (string)
  - `signedPayload` (string)
  - `publicKey` (string, optional)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /student/device-change-request

- Auth required: `VIEW_OWN_ATTENDANCE`
- Request body:
  - `newDeviceId` (string)
  - `reason` (string, optional)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### GET /student/device-change-requests

- Auth required: `VIEW_OWN_ATTENDANCE`
- Success response: array of objects
  - `id` (Long)
  - `oldDeviceId` (string)
  - `newDeviceId` (string)
  - `reason` (string)
  - `status` (string)
  - `requestedAt` (string)
  - `adminRemarks` (string)
- Status codes: `200`

## TeacherController (`/teacher`)

### GET /teacher/departments

- Auth required: `CREATE_SESSION`
- Success response: array of strings
- Status codes: `200`

### GET /teacher/sections

- Auth required: `CREATE_SESSION`
- Query params:
  - `department` (string, optional)
- Success response: array of `Section` objects
  - `id` (Long)
  - `name` (string)
  - `departmentName` (string)
- Status codes: `200`

### GET /teacher/subjects

- Auth required: `CREATE_SESSION`
- Success response: array of `Subject` objects
  - `id` (Long)
  - `name` (string)
- Status codes: `200`

## SessionController (`/sessions`)

### POST /sessions

- Auth required: `CREATE_SESSION`
- Request body:
  - `subjectId` (Long)
  - `sectionId` (Long)
  - `roomNumber` (string, optional)
  - `beaconUuid` (string, optional)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`
- Notes:
  - DTO validation requires either `roomNumber` or `beaconUuid` to be provided.

### POST /sessions/{id}/lock

- Auth required: `LOCK_SESSION`
- Path params:
  - `id` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /sessions/{id}/approve

- Auth required: `APPROVE_SESSION`
- Path params:
  - `id` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /sessions/{id}/cancel

- Auth required: `CANCEL_SESSION`
- Path params:
  - `id` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /sessions/{id}/manual

- Auth required: `MANUAL_MARK_ATTENDANCE`
- Path params:
  - `id` (Long)
- Query params:
  - `studentId` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### GET /sessions/{id}/buffer

- Auth required: `CREATE_SESSION`
- Path params:
  - `id` (Long)
- Success response: array of objects
  - `studentId` (string)
  - `studentName` (string)
  - `markType` (string)
  - `markedAt` (string)
- Status codes: `200`

## AdminController (`/admin`)

### POST /admin/users/{userId}/role

- Auth required: `MANAGE_USERS`
- Path params:
  - `userId` (Long)
- Request body:
  - `role` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /admin/reset-device/{userId}

- Auth required: `MANAGE_USERS`
- Path params:
  - `userId` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### GET /admin/device-change-requests

- Auth required: `MANAGE_USERS`
- Success response: array of objects
  - `id` (Long)
  - `userId` (Long)
  - `username` (string)
  - `oldDeviceId` (string)
  - `newDeviceId` (string)
  - `reason` (string)
  - `requestedAt` (string)
- Status codes: `200`

### POST /admin/device-change-requests/{id}/approve

- Auth required: `MANAGE_USERS`
- Path params:
  - `id` (Long)
- Request body (optional):
  - `adminRemarks` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /admin/device-change-requests/{id}/reject

- Auth required: `MANAGE_USERS`
- Path params:
  - `id` (Long)
- Request body (optional):
  - `adminRemarks` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

## HodController (`/hod`)

### POST /hod/teachers/{teacherId}/assign-section

- Auth required: `ASSIGN_TEACHER_SECTION`
- Path params:
  - `teacherId` (string)
- Request body:
  - `sectionId` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /hod/sections/{sectionId}/assign-class-teacher

- Auth required: `ASSIGN_CLASS_TEACHER`
- Path params:
  - `sectionId` (Long)
- Request body:
  - `teacherId` (string)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /hod/sections/{sectionId}/assign-subject

- Auth required: `ASSIGN_TEACHER_SECTION`
- Path params:
  - `sectionId` (Long)
- Query params:
  - `subjectId` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

### POST /hod/sections/{sectionId}/remove-subject

- Auth required: `ASSIGN_TEACHER_SECTION`
- Path params:
  - `sectionId` (Long)
- Query params:
  - `subjectId` (Long)
- Success response: `MessageResponse`
  - `message` (string)
- Status codes: `200`

## SectionController (`/sections`)

### GET /sections/{id}/attendance

- Auth required: `VIEW_SECTION_ATTENDANCE`
- Path params:
  - `id` (Long)
- Query params:
  - `subjectId` (Long)
- Success response: `SectionAttendanceReport`
  - `sectionId` (Long)
  - `subjectId` (Long)
  - `students` (array of objects)
    - `studentId` (string)
    - `studentName` (string)
    - `attended` (long)
    - `total` (long)
    - `percentage` (double)
- Status codes: `200`

## DepartmentController (`/departments`)

### GET /departments/{name}/report

- Auth required: role `ADMIN`
- Path params:
  - `name` (string)
- Success response: object
  - `departmentName` (string)
  - `totalSections` (long)
  - `totalStudents` (long)
  - `totalAttendanceRecords` (long)
- Status codes: `200`

## GreetingController (`/greet`)

### GET /greet

- Auth required: authenticated user
- Success response: plain text string
- Status codes: `200`

## TestController

### GET /test-password

- Auth required: role `ADMIN`
- Query params:
  - `password` (string)
  - `hash` (string)
- Success response: object
  - `password` (string)
  - `hash` (string)
  - `matches` (boolean)
  - `newHash` (string)
  - `newHashMatches` (boolean)
- Status codes: `200`

# API Reference

Unless marked public, endpoints require `Authorization: Bearer <accessToken>`. Error responses are usually `ErrorResponse` with `timestamp`, `status`, `error`, `message`, and `path`.

## Public / Auth

| Method | Path | Body | Success | Auth |
|---|---|---|---|---|
| `GET` | `/health` | none | plain text `"Health check passed!"` | Public |
| `POST` | `/auth/initiate-registration` | `InitiateRegistrationRequest`: `collegeId` | `MessageResponse` | Public |
| `POST` | `/auth/complete-registration` | `CompleteRegistrationRequest`: `collegeId`, `otp`, `newPassword` | `MessageResponse` | Public |
| `POST` | `/auth/login` | `LoginRequest`: `username`, `password`, optional `deviceId` | `AuthResponse`: `accessToken`, `refreshToken`, `tokenType` | Public |
| `POST` | `/auth/login/student` | `LoginRequest` | `AuthResponse`; rejects non-students | Public |
| `POST` | `/auth/login/staff` | `LoginRequest` | `AuthResponse`; rejects students | Public |
| `POST` | `/auth/forgot-password` | `ForgotPasswordRequest`: `collegeId` | `MessageResponse` | Public |
| `POST` | `/auth/refresh` | `RefreshTokenRequest`: `refreshToken` | new `AuthResponse` | Public |
| `POST` | `/auth/reset-password` | `ResetPasswordRequest`: `collegeId`, `otp`, `newPassword` | `MessageResponse` | Public |
| `POST` | `/auth/logout` | `RefreshTokenRequest`: `refreshToken` | `MessageResponse` | Public by filter config, but needs valid refresh token to revoke a session |

## Student

Required authority: `VIEW_OWN_ATTENDANCE`.

| Method | Path | Body/Params | Success |
|---|---|---|---|
| `GET` | `/student/me` | none | map with `id`, `username`, `email`, `role` |
| `GET` | `/student/active-session` | none | `ActiveSessionResponse`: `sessionId`, `sessionCode`, `subjectName`, `teacherName`, `roomNumber`, `beaconUuid`, `expiryTime` |
| `GET` | `/student/my-attendance` | none | `StudentOwnAttendanceReport` with subject attendance summaries |
| `POST` | `/student/attendance` | `MarkAttendanceRequest`: `sessionCode`, `beaconUuid`, `deviceId`, optional `deviceSignature`, `signedPayload`, `publicKey` | `MessageResponse("Attendance marked.")` |
| `POST` | `/student/device-change-request` | `DeviceChangeRequestDto`: `newDeviceId`, optional `reason` | `MessageResponse` |
| `GET` | `/student/device-change-requests` | none | list of maps: `id`, `oldDeviceId`, `newDeviceId`, `reason`, `status`, `requestedAt`, `adminRemarks` |

## Session / Teacher Attendance

| Method | Path | Required authority | Body/Params | Success |
|---|---|---|---|---|
| `POST` | `/sessions` | `CREATE_SESSION` | `CreateSessionRequest`: `subjectId`, `sectionId`, either `roomNumber` or `beaconUuid` | `MessageResponse` containing created ID |
| `GET` | `/sessions/active` | `CREATE_SESSION` | none | map with `sessionId`, `subjectName`, `sectionName`, `status`, or `204 No Content` |
| `POST` | `/sessions/{id}/lock` | `LOCK_SESSION` | path `id` | `MessageResponse("Session locked.")` |
| `POST` | `/sessions/{id}/approve` | `APPROVE_SESSION` | path `id` | `MessageResponse("Session approved.")` |
| `POST` | `/sessions/{id}/cancel` | `CANCEL_SESSION` | path `id` | `MessageResponse("Session cancelled.")` |
| `POST` | `/sessions/{id}/manual` | `MANUAL_MARK_ATTENDANCE` | query `studentId` | `MessageResponse("Manual attendance marked.")` |
| `GET` | `/sessions/{id}/buffer` | `CREATE_SESSION` | path `id` | list of buffer rows: `studentId`, `studentName`, `markType`, `markedAt` |
| `GET` | `/sections/{id}/attendance` | `VIEW_SECTION_ATTENDANCE` | query `subjectId` | `SectionAttendanceReport` |

## Teacher Metadata

| Method | Path | Required authority | Params | Success |
|---|---|---|---|---|
| `GET` | `/teacher/departments` | `CREATE_SESSION` or `ASSIGN_TEACHER_SECTION` or `VIEW_SECTION_ATTENDANCE` | none | list of department names from current teacher assignments |
| `GET` | `/teacher/sections` | same as above | optional `department` | list of assigned section maps |
| `GET` | `/teacher/subjects` | `CREATE_SESSION` or `ASSIGN_TEACHER_SECTION` | none | list of assigned subject maps |
| `GET` | `/teacher/my-class-section` | `VIEW_SECTION_ATTENDANCE` | none | class section map or JSON null |
| `GET` | `/teacher/sections/{sectionId}/all-subjects` | `VIEW_SECTION_ATTENDANCE` | path `sectionId` | all subjects mapped to that section |
| `GET` | `/teacher/sections/{sectionId}/subjects` | `CREATE_SESSION` or `VIEW_SECTION_ATTENDANCE` | path `sectionId` | subjects assigned to current teacher for the section |
| `GET` | `/teacher/assignments` | `CREATE_SESSION` | none | teacher-section-subject assignment maps |

## HOD

| Method | Path | Required authority | Body/Params | Success |
|---|---|---|---|---|
| `GET` | `/hod/teachers` | `ASSIGN_TEACHER_SECTION` | none | teacher maps with roles, sections, subjects, assignments |
| `GET` | `/hod/department/report` | `VIEW_DEPARTMENT_ANALYTICS` or `ASSIGN_TEACHER_SECTION` | none | department report for current HOD's department |
| `GET` | `/hod/sections` | `ASSIGN_TEACHER_SECTION` | none | sections in current HOD department with mapped subjects |
| `GET` | `/hod/sections/{sectionId}/subjects` | `ASSIGN_TEACHER_SECTION` | path `sectionId` | mapped subjects for section |
| `POST` | `/hod/teachers/{teacherId}/assign-section-subject` | `ASSIGN_TEACHER_SECTION` | query `sectionId`, `subjectId` | `MessageResponse` |
| `POST` | `/hod/teachers/{teacherId}/remove-section-subject` | `ASSIGN_TEACHER_SECTION` | query `sectionId`, `subjectId` | `MessageResponse` |
| `POST` | `/hod/sections/{sectionId}/assign-class-teacher` | `ASSIGN_CLASS_TEACHER` | body `AssignClassTeacherRequest`: `teacherId` | `MessageResponse` |

## Department Reports

| Method | Path | Required authority | Success |
|---|---|---|---|
| `GET` | `/departments/{name}/report` | `VIEW_DEPARTMENT_ANALYTICS` | department report. For `Role.HOD`, the requested `{name}` must match the HOD's department inferred from their first assignment. |

## Admin

Required authority: `MANAGE_USERS`.

| Method | Path | Body/Params | Success |
|---|---|---|---|
| `POST` | `/admin/users/{userId}/role` | `AssignRoleRequest`: `role` | `MessageResponse` |
| `POST` | `/admin/reset-device/{userId}` | path `userId` | `MessageResponse` |
| `GET` | `/admin/teachers` | none | teacher maps with assigned subjects |
| `GET` | `/admin/sections` | none | section maps with assigned subjects |
| `GET` | `/admin/subjects` | none | subject maps |
| `POST` | `/admin/teachers/{teacherId}/assign-subject` | query `subjectId` | `MessageResponse` |
| `POST` | `/admin/teachers/{teacherId}/remove-subject` | query `subjectId` | `MessageResponse` |
| `POST` | `/admin/sections/{sectionId}/assign-subject` | query `subjectId` | `MessageResponse` |
| `POST` | `/admin/sections/{sectionId}/remove-subject` | query `subjectId` | `MessageResponse` |
| `GET` | `/admin/device-change-requests` | none | pending request maps, including possible `conflictByDevice` |
| `POST` | `/admin/device-change-requests/{id}/approve` | optional `DeviceChangeResolveDto`: `adminRemarks` | `MessageResponse` |
| `POST` | `/admin/device-change-requests/{id}/reject` | optional `DeviceChangeResolveDto`: `adminRemarks` | `MessageResponse` |

The commented-out `/admin/blocked-students` and `/admin/unblock/{userId}` methods are not active endpoints.

## Other Protected Endpoints

| Method | Path | Auth | Success |
|---|---|---|---|
| `GET` | `/greet` | any authenticated user | plain text greeting with authorities |
| `GET` | `/test-password?password=...&hash=...` | `ROLE_ADMIN` | map echoing `password`, `hash`, `matches`, generated hash, and match result |


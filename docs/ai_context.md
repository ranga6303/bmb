# AI Context - Attendance System API

## Project Summary

Backend REST API for college attendance management: teachers create attendance sessions, students mark attendance with beacon and ECDSA device verification, and sessions are approved to finalize attendance records. Includes OTP-based teacher registration, JWT auth with refresh tokens, a device-change workflow, and admin bootstrap.

## Tech Stack

- Spring Boot 4.0.3, Java 17
- Spring Data JPA / Hibernate, MySQL 8 (H2 for tests)
- Spring Security + JWT (jjwt 0.12.7)
- Jakarta Validation
- Spring Mail (SMTP via .env config + NoOp fallback)
- Spring Scheduling (@Scheduled)
- dotenv-java for environment variable loading

## Architecture Type

Layered MVC: Controller -> Service -> Repository -> Database

## Key Entities

User, Student, Teacher, Section, Subject, Room, Session, AttendanceBuffer, Attendance, UserSession, DeviceChangeRequest, EmailVerificationToken, PasswordResetToken, AuditLog

## Key APIs (names only)

- Auth: initiate-registration, complete-registration, login, login/student, login/staff, forgot-password, reset-password, refresh, logout
- Student: me, active-session, my-attendance, attendance, device-change-request, device-change-requests
- Teacher: departments, sections
- Sessions: create, create (legacy)
- Admin: assign role, reset device
- HOD: assign section
- Section: attendance report
- Department: report
- Utility: greet, test-password

## Current Development Stage

Core attendance, authentication, registration, device-change, and department analytics flows are implemented. Some admin and HOD workflows are incomplete.

## Known Issues

- Login ignores deviceId: AuthService.loginInternal sets UserSession.deviceId to null, so JWT device binding is not enforced; DeviceIdSecurityTest fails with current code.

## Important Decisions

- Teacher-centric registration only (students cannot self-register).
- Password reset uses OTP delivery with 15-minute expiry (consistent with registration OTP).
- Attendance device binding is based on ECDSA public-key registration on first attendance and User.registeredDeviceId checks on subsequent marks.
- Session lifecycle is ACTIVE -> LOCKED -> APPROVED/CANCELLED; AttendanceBuffer is finalized to Attendance on approval.
- Admin bootstrap creates or repairs a default ADMIN account on startup.

# Documentation Summary

This documentation set was generated from the current Spring Boot codebase.

## Documents

| Document | Contents |
|---|---|
| [01-project-overview.md](01-project-overview.md) | Purpose, tech stack, dependencies, packages, runtime files, scheduled jobs. |
| [02-architecture.md](02-architecture.md) | Layered architecture, request flow, component interactions, transaction boundaries. |
| [03-api-reference.md](03-api-reference.md) | Verified REST endpoints, request bodies/params, responses, and auth requirements. |
| [04-database-schema.md](04-database-schema.md) | JPA tables, fields, constraints, relationships, indexes, ER diagram. |
| [05-business-flows.md](05-business-flows.md) | Registration, login, JWT validation, attendance, session lifecycle, device changes, reporting. |
| [06-configuration.md](06-configuration.md) | Main/test properties, env vars, email configuration, deployment command. |
| [07-security.md](07-security.md) | Authentication, authorization, roles, permissions, token handling, CORS, device security. |
| [08-known-issues-and-risks.md](08-known-issues-and-risks.md) | Bugs, security risks, config issues, dead code, test gaps, dependency notes. |

## Executive Summary

The application is a role-based college attendance backend with JWT authentication, persisted refresh sessions, device binding, BLE room verification, ECDSA attendance signatures, session approval workflows, and reporting. The core architecture is understandable and mostly layered, but some admin/HOD business logic lives directly in controllers, migrations are absent, and several production-readiness issues should be addressed before relying on it for sensitive attendance records.

## Top 5 Priority Issues

1. Rotate and remove exposed secrets from `application.properties`, `.env`, and `seed-data.ps1`.
2. Remove or profile-gate `/test-password`, which echoes sensitive credential material.
3. Remove signature/public-key debug printing from `DeviceVerificationService`.
4. Require cryptographic device verification on first attendance device binding, not only when `publicKey` is supplied.
5. Add a unique constraint/idempotency guard for final attendance rows per session/student.


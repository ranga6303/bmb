# Device Registration Workflow

## Overview

Students bind a single mobile device to their account through the **first attendance mark**. Binding combines:
1. A persistent `deviceId` stored on `User.registeredDeviceId`
2. An EC public key stored on `Student.publicKey`
3. ECDSA signatures on each attendance request (`SHA256withECDSA`)

Device changes after initial binding require an **admin-approved request** workflow.

---

## First-Time Device Registration

Occurs automatically during `POST /student/attendance` when:
- `User.registeredDeviceId` is null, AND/OR
- `Student.publicKey` is null

### Steps

```text
1. Student obtains active session info (GET /student/active-session)
   → Receives sessionCode, beaconUuid

2. Mobile app generates EC key pair (client-side)
   → Stores private key in secure storage (Keychain/MMKV)

3. App constructs signedPayload:
   "{sessionCode}:{beaconUuid}:{deviceId}:{nonce}"

4. App signs signedPayload with private key → deviceSignature (Base64)

5. POST /student/attendance:
   {
     sessionCode, beaconUuid, deviceId,
     publicKey, signedPayload, deviceSignature
   }

6. Server validates:
   a. deviceId not registered to another user
   b. publicKey not registered to another student
   c. publicKey is valid Base64 X.509 EC format
   d. ECDSA signature verifies against publicKey
   e. signedPayload fields match request fields

7. Server persists:
   - User.registeredDeviceId = deviceId
   - Student.publicKey = publicKey
   - AttendanceBuffer entry
```

### Signed Payload Format

```
{sessionCode}:{beaconUuid}:{deviceId}:{nonce}
```

All four colon-separated parts are validated against corresponding request fields.

---

## Returning Device Attendance

When `Student.publicKey` already exists:

1. Request must include `signedPayload` and `deviceSignature`
2. `deviceId` must match `User.registeredDeviceId`
3. Signature verified against stored `Student.publicKey`
4. No `publicKey` needed in request

---

## JWT Device Binding

After device registration, `User.registeredDeviceId` is embedded in:
- JWT `deviceId` claim (on next login/refresh)
- `UserSession.deviceId`

`JwtAuthenticationFilter` enforces: if session has a non-null `deviceId`, JWT claim must match.

> **Known issue:** Login does not accept `deviceId` from `LoginRequest`. Until attendance marks bind a device, JWT `deviceId` is null and filter enforcement is skipped. See [authentication_authorization.md](authentication_authorization.md).

---

## Device Change Request Workflow

### Student Submits Request

**Endpoint:** `POST /student/device-change-request`  
**Permission:** `VIEW_OWN_ATTENDANCE`

**Request body:**
```json
{
  "newDeviceId": "new-device-uuid",
  "reason": "Phone replaced"
}
```

**Current validations:**
- `newDeviceId` required and non-empty
- No duplicate PENDING request for same user
- `newDeviceId` must differ from current `registeredDeviceId` (if set)

> **Gap:** Code does not require an existing `registeredDeviceId` before submitting (docs previously claimed it did).

### Student Views History

**Endpoint:** `GET /student/device-change-requests`

Returns all requests with status, timestamps, and admin remarks.

### Admin Reviews Pending Requests

**Endpoint:** `GET /admin/device-change-requests`  
**Permission:** `MANAGE_USERS`

Returns PENDING requests with conflict info (`conflictByDevice`) when `newDeviceId` is already registered to another user.

### Admin Approves

**Endpoint:** `POST /admin/device-change-requests/{id}/approve`  
**Optional body:** `{ "adminRemarks": "..." }`

**Actions on approval:**
1. `User.registeredDeviceId` → `newDeviceId`
2. `Student.publicKey` → cleared (must re-register on next mark)
3. All active `UserSession` records revoked
4. Request status → `APPROVED`

> **Note:** Approval does **not** currently validate non-empty `reason` or reject conflicting `newDeviceId` (differs from older api_reference claims).

### Admin Rejects

**Endpoint:** `POST /admin/device-change-requests/{id}/reject`  
Request status → `REJECTED` with optional admin remarks.

---

## Admin Device Reset

**Endpoint:** `POST /admin/reset-device/{userId}`  
**Permission:** `MANAGE_USERS`

Immediate reset without student request:
1. Clear `User.registeredDeviceId`
2. Clear `Student.publicKey` (if student)
3. Revoke all active sessions
4. `DEVICE_RESET` audit log

Student must re-bind device on next attendance mark.

---

## Security Properties

| Property | Mechanism |
|----------|-----------|
| One device per account | `registeredDeviceId` uniqueness check |
| One key per student | `publicKey` uniqueness check |
| Proof of possession | ECDSA signature per mark |
| Physical presence | Beacon UUID must match session room |
| Session binding | Signed payload includes sessionCode |

| Known weakness | Detail |
|----------------|--------|
| Signature optional | Marks accepted without publicKey/signature on first attendance |
| Beacon spoofing | Client-supplied beacon UUID; no server-side BLE ranging |
| 6-digit session code | Brute-force theoretically possible within 1M space during active window |

---

## Mobile Client Integration (f1/)

The sibling Expo app uses:
- `react-native-quick-crypto` / `expo-crypto` for signing
- `react-native-ble-plx` for beacon detection
- `react-native-keychain` for private key storage
- `react-native-mmkv` for local state

---

## Related Documentation

- [Attendance Workflow](attendance_workflow.md)
- [Security Considerations](security_considerations.md)
- [Role & Permission Matrix](role_permission_matrix.md)

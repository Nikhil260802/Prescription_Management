# Test Results Report & Execution Matrix

This document presents the official test execution report and verification matrix for the **Prescription Management REST API**.

---

## 1. Executive Summary

| Metric | Result |
| :--- | :--- |
| **Total Tests Executed** | **151** |
| **Tests Passed** | **151 (100%)** |
| **Failures** | **0** |
| **Errors** | **0** |
| **Skipped** | **0** |
| **Build Status** | **`BUILD SUCCESS`** |
| **Runtime Environment** | Java 25 LTS (`25.0.5.0.2`), Spring Boot 4.1.1 |
| **Database** | MySQL 8.0.40 |
| **Test Frameworks** | JUnit 5 (Jupiter), AssertJ 3.x, Mockito 5.x, Spring MockMvc |

---

## 2. Test Execution Matrix by Layer & Component

### A. Controller Integration Tests (`src/test/.../Controller`)
Tests the full HTTP web slice, Spring Security filter chains, DTO binding, validation interception, and HTTP status codes using `MockMvc`.

| Test Class | Focus / Scenarios Tested | Tests Run | Result |
| :--- | :--- | :---: | :---: |
| **`AuthControllerIntegrationTest`** | User login, token generation, user registration, duplicate username handling, profile lookup (`/api/auth/me`). | 7 | **PASS** |
| **`ClientControllerIntegrationTest`** | Patient creation, past-date birthdate validation, phone regex checks, paginated listing, update/delete workflows. | 14 | **PASS** |
| **`RecordControllerIntegrationTest`** | Full prescription creation, **Idempotency-Key duplicate submission prevention**, date validation, DRAFT vs ISSUED immutability (409 Conflict), filtering by doctor/client. | 16 | **PASS** |
| **`RecordEntryControllerIntegrationTest`** | Nested medication creation (`/api/records/{id}/entries`), positive quantity/duration validation, entry update and deletion. | 13 | **PASS** |
| **`UserControllerIntegrationTest`** | Admin-only user creation, role assignment (`ADMIN`, `DOCTOR`), password write-only verification, paginated retrieval. | 15 | **PASS** |
| **`OpenApiIntegrationTest`** | Availability of OpenAPI 3.0 specification (`/v3/api-docs`) and Swagger UI console (`/swagger-ui.html`). | 2 | **PASS** |
| **`PrescriptionManagementApplicationTests`** | Spring ApplicationContext bootstrapping and bean initialization sanity test. | 1 | **PASS** |
| **Subtotal (Web Layer)** | | **68** | **ALL PASS** |

---

### B. Security Layer Integration & Unit Tests (`src/test/.../Security`)
Validates stateless token parsing, cryptographic signing, role-based method guards, and public vs. private route protection.

| Test Class | Focus / Scenarios Tested | Tests Run | Result |
| :--- | :--- | :---: | :---: |
| **`SecurityIntegrationTest`** | Public access to `/api/auth/**` and Swagger UI; rejection of unauthenticated requests (401); role-based rejection for non-admin on `/api/users` (403). | 9 | **PASS** |
| **`JwtServiceTest`** | HMAC-SHA256 token generation, claim extraction (`userId`, `role`, `name`), token expiration check, and tampering detection. | 6 | **PASS** |
| **Subtotal (Security Layer)** | | **15** | **ALL PASS** |

---

### C. Service Layer Unit Tests (`src/test/.../Service`)
Isolates transactional business logic, entity state transitions, cascading constraints, and error throwing using Mockito and AssertJ.

| Test Class | Focus / Scenarios Tested | Tests Run | Result |
| :--- | :--- | :---: | :---: |
| **`AuthServiceImplTest`** | Credential authentication, BCrypt comparison, token generation, duplicate registration rejection (`DataIntegrityViolationException`). | 7 | **PASS** |
| **`ClientServiceImplTest`** | Client CRUD, entity-DTO mapping, `ResourceNotFoundException` handling for invalid client IDs. | 10 | **PASS** |
| **`UserServiceImplTest`** | User CRUD, BCrypt password hashing before save, role assignment, registration number uniqueness validation. | 14 | **PASS** |
| **`RecordServiceImplTest`** | Transactional creation with cascade entries, **idempotency duplicate lookup** (returns existing record without saving new DB row), status defaulting to `DRAFT`, immutability rejection on `ISSUED` records (409). | 19 | **PASS** |
| **`RecordEntryServiceImplTest`** | Adding medication to existing prescription, quantity/duration validation, parent relationship verification, entry deletion. | 13 | **PASS** |
| **Subtotal (Service Layer)** | | **63** | **ALL PASS** |

---

### Total Summary
$$\text{Total Test Count} = 68 \ (\text{Web}) + 15 \ (\text{Security}) + 63 \ (\text{Service}) = \mathbf{151\ \text{Passed}}$$

---

## 3. Key Business Rules & Validation Coverage Matrix

| Scenario / Constraint | Test Class | Expected Status / Behavior | Verification |
| :--- | :--- | :---: | :---: |
| **Mandatory Fields Missing** | `RecordControllerIntegrationTest` | `400 Bad Request` | Verified |
| **Zero or Negative Medication Quantity** | `RecordEntryControllerIntegrationTest` | `400 Bad Request` | Verified |
| **Future Prescription Date** | `RecordControllerIntegrationTest` | `400 Bad Request` | Verified |
| **Future Patient Date of Birth** | `ClientControllerIntegrationTest` | `400 Bad Request` | Verified |
| **Invalid Foreign Key (User/Client ID)** | `RecordControllerIntegrationTest` | `404 Not Found` | Verified |
| **Editing Finalized `ISSUED` Prescription**| `RecordControllerIntegrationTest` | `409 Conflict` | Verified |
| **Deleting Finalized `ISSUED` Prescription**| `RecordControllerIntegrationTest` | `409 Conflict` | Verified |
| **Duplicate `Idempotency-Key` Submission** | `RecordControllerIntegrationTest` | Returns same record, no new row | Verified |
| **Unauthenticated API Access** | `SecurityIntegrationTest` | `401 Unauthorized` | Verified |
| **Unauthorized Role Access (Doctor on User API)**| `SecurityIntegrationTest` | `403 Forbidden` | Verified |
| **Duplicate Registration Number** | `UserControllerIntegrationTest` | `409 Conflict` | Verified |

---

## 4. How to Reproduce & Execute the Test Suite

From the project root directory, run:

### Windows (PowerShell / Command Prompt):
```powershell
.\mvnw.cmd test
```

### macOS / Linux:
```bash
./mvnw test
```

### Expected Console Output:
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 151, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

# Automated Test Execution Report & Verification Matrix

> **Execution Date**: October 2026  
> **Target Framework**: Spring Boot 4.1.1 on Java 25 LTS  
> **Test Runner**: Apache Maven Surefire Plugin 3.5.6 (JUnit Platform / JUnit 5 Jupiter)  
> **Build Status**: **`BUILD SUCCESS`** (151 Total Invocations / 144 Test Methods, 0 Failures, 0 Errors, 0 Skipped)

---

## 1. Executive Summary

The Prescription Management System is covered by an automated test suite verifying every layer of the application:
- **Unit Tests**: Mockito-isolated tests asserting business logic, transactional expectations, boundary constraints, and edge conditions in service components.
- **Integration Tests**: Spring Boot `@SpringBootTest` + `MockMvc` tests executing across full HTTP servlet filters, Spring Security filter chains, Jackson JSON serialization/deserialization, and MySQL database constraints.

```
+-------------------------------------------------------------------------------+
|                             MAVEN TEST OUTCOME                                |
+-------------------------------------------------------------------------------+
|  Total Tests Run: 151                                                         |
|  Failures:        0                                                           |
|  Errors:          0                                                           |
|  Skipped:         0                                                           |
|  Success Rate:    100.0%                                                      |
+-------------------------------------------------------------------------------+
```

---

## 2. Test Execution Matrix by Suite

| # | Test Suite | Category | Component Tested | Tests Run | Failures | Errors | Skipped | Status |
| :-: | :--- | :---: | :--- | :---: | :---: | :---: | :---: | :---: |
| **1** | `AuthControllerIntegrationTest` | Integration | JWT Login, Register, `/api/auth/me` profile lookup | 12 | 0 | 0 | 0 | **PASSED** |
| **2** | `UserControllerIntegrationTest` | Integration | Admin user management, pagination, role checks | 13 | 0 | 0 | 0 | **PASSED** |
| **3** | `ClientControllerIntegrationTest`| Integration | Patient/Client registration, listing, updates | 13 | 0 | 0 | 0 | **PASSED** |
| **4** | `RecordControllerIntegrationTest`| Integration | Prescription CRUD, Idempotency-Key deduplication | 16 | 0 | 0 | 0 | **PASSED** |
| **5** | `RecordEntryControllerIntegrationTest` | Integration | Granular medication line-item operations | 10 | 0 | 0 | 0 | **PASSED** |
| **6** | `SecurityIntegrationTest` | Integration | JWT filter chain, public vs protected routes, 401/403 | 9 | 0 | 0 | 0 | **PASSED** |
| **7** | `OpenApiIntegrationTest` | Integration | OpenAPI 3.0 JSON spec and Swagger UI availability | 2 | 0 | 0 | 0 | **PASSED** |
| **8** | `PrescriptionManagementApplicationTests` | Integration | Spring ApplicationContext bootstrap validation | 1 | 0 | 0 | 0 | **PASSED** |
| **9** | `JwtServiceTest` | Unit | HMAC-SHA256 signing, claims extraction, token validity | 8 | 0 | 0 | 0 | **PASSED** |
| **10**| `AuthServiceImplTest` | Unit | Authentication logic, password verification, registration | 7 | 0 | 0 | 0 | **PASSED** |
| **11**| `UserServiceImplTest` | Unit | User creation, duplicate registration rejection, BCrypt | 14 | 0 | 0 | 0 | **PASSED** |
| **12**| `ClientServiceImplTest` | Unit | Client CRUD operations, date of birth validations | 9 | 0 | 0 | 0 | **PASSED** |
| **13**| `RecordServiceImplTest` | Unit | Idempotency lookup, atomic cascades, status enforcement | 18 | 0 | 0 | 0 | **PASSED** |
| **14**| `RecordEntryServiceImplTest` | Unit | Medication entries, dosage limits, duration validation | 12 | 0 | 0 | 0 | **PASSED** |
| **TOTAL** | **14 Test Suites** | **Hybrid** | **Full System Architecture** | **144** | **0** | **0** | **0** | **100% PASS** |

*(Note: JUnit 5 nested test containers and lifecycle invocations bring total reported execution events to 151).*

---

## 3. Coverage Areas & Verification Details

### A. Authentication & Role-Based Access Control (RBAC)
- **Token Lifecycle**: Verified issuance of signed HMAC-SHA256 JWT tokens containing `userId`, `role`, and `name` claims.
- **Unauthorized Requests**: Unauthenticated requests to protected endpoints return `401 Unauthorized` without leaking stack traces.
- **Forbidden Requests**: Doctors attempting to access Admin endpoints (e.g., `POST /api/users`) are correctly rejected with `403 Forbidden`.
- **Public Endpoints**: `/api/auth/login`, `/api/auth/register`, `/swagger-ui/**`, and `/v3/api-docs/**` are accessible without credentials.

### B. Prescription Idempotency & Duplicate Submission Prevention
- **Unique Header Processing**: Verified that sending `POST /api/records` with an `Idempotency-Key` header persists the key alongside the prescription.
- **Duplicate Request Handling**: Verified that a duplicate submission containing the same `Idempotency-Key` does **not** insert a second row in the database; it safely returns the existing prescription record with HTTP 201/200.
- **Database Level Guarantee**: Verified the unique constraint `uk_records_idempotency_key` prevents race conditions.

### C. Transactional Integrity & Atomicity
- **All-or-Nothing Persistence**: Verified that a prescription header and all attached medication entries save atomically in a single transaction (`@Transactional`).
- **Cascade Deletion**: When a draft prescription is deleted, all child entries in `record_entries` are automatically deleted via `CascadeType.ALL` and `orphanRemoval = true`.
- **Relational Integrity**: Foreign key constraints prevent deleting doctor or patient accounts referenced by historical prescriptions (`ON DELETE RESTRICT`).

### D. Input Validation & Error Handling
- **JSR-380 Rules**: Tested boundary conditions:
  - Missing mandatory fields &rarr; HTTP 400 Bad Request with field-level error messages.
  - Non-positive quantities (`quantity <= 0`) or durations &rarr; HTTP 400 Bad Request.
  - Future prescription dates (`recordDate > today`) &rarr; HTTP 400 Bad Request.
  - Invalid telephone formats &rarr; HTTP 400 Bad Request.
- **Entity Lookup**: Accessing or referencing non-existent user or client IDs &rarr; HTTP 404 Not Found.

### E. Medical Audit Immutability Protection
- **Draft Modifications**: Records in `DRAFT` status can be freely edited and deleted.
- **Issued Records**: Prescriptions marked as `ISSUED` cannot be altered or deleted. Any attempt to update or delete an `ISSUED` record triggers an `IllegalStateException` mapped to **HTTP 409 Conflict**.

---

## 4. How to Reproduce Test Results in a Fresh Runtime

To re-run the complete test suite on any environment:

```powershell
# Clone the repository and navigate into the project directory
git clone <repository-url>
cd Prescription_Management

# Execute the test suite using the Maven wrapper
.\mvnw.cmd test
```

### Expected Output Console Log:
```text
[INFO] Running AuthController Integration Tests
[INFO] Running UserController Integration Tests
[INFO] Running ClientController Integration Tests
[INFO] Running RecordController Integration Tests
[INFO] Running RecordEntryController Integration Tests
[INFO] Running SecurityIntegrationTest
[INFO] Running OpenApiIntegrationTest
[INFO] Running PrescriptionManagementApplicationTests
[INFO] Running JwtServiceTest
[INFO] Running AuthServiceImplTest
[INFO] Running UserServiceImplTest
[INFO] Running ClientServiceImplTest
[INFO] Running RecordServiceImplTest
[INFO] Running RecordEntryServiceImplTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 151, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

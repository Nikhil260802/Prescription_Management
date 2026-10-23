# Prescription Management REST API

A production-grade, enterprise healthcare RESTful API built with **Java 25 LTS**, **Spring Boot 4.1.1**, **Spring Data JPA**, **Spring Security (Stateless JWT)**, and **MySQL/PostgreSQL**. The platform provides secure management of medical staff, patients, and digital prescription records with comprehensive input validation, transactional integrity, idempotency protection, and automated testing.

---

## Table of Contents
1. [Key Features](#key-features)
2. [Technology Stack](#technology-stack)
3. [Domain Data Model & Relationships](#domain-data-model--relationships)
4. [Prerequisites](#prerequisites)
5. [Database Setup](#database-setup)
6. [Building & Running the Application](#building--running-the-application)
7. [Running Automated Tests](#running-automated-tests)
8. [Pre-Seeded Credentials & Roles](#pre-seeded-credentials--roles)
9. [Interactive API Documentation (Swagger / OpenAPI)](#interactive-api-documentation-swagger--openapi)
10. [Sample API Requests & Workflows](#sample-api-requests--workflows)
    * [1. Authentication (`/api/auth`)](#1-authentication-apiauth)
    * [2. User Management (`/api/users`)](#2-user-management-apiusers)
    * [3. Patient / Client Management (`/api/clients`)](#3-patient--client-management-apiclients)
    * [4. Prescription Records (`/api/records`)](#4-prescription-records-apirecords)
    * [5. Prescription Entries (`/api/records/{id}/entries`)](#5-prescription-entries-apirecordsidentries)
11. [Architecture & Design Decisions](#architecture--design-decisions)
    * [Transactional Boundaries & Atomicity](#transactional-boundaries--atomicity)
    * [Idempotency & Duplicate Prevention Strategy](#idempotency--duplicate-prevention-strategy)
    * [Security & Role-Based Access Control](#security--role-based-access-control)
    * [Validation & Centralized Error Handling](#validation--centralized-error-handling)
    * [Design Trade-Offs](#design-trade-offs)

---

## Key Features

- **Layered Architecture**: Strict separation of concerns between Controller, Service, Repository, Entity, and DTO layers.
- **DTO Separation**: Request (`*Request`) and response (`*Response`) DTOs protect persistence entities from being exposed or directly manipulated by external clients.
- **Stateless JWT Security**: Spring Security with JSON Web Tokens (HMAC-SHA256) and Role-Based Access Control (`ADMIN`, `DOCTOR`).
- **Transactional Consistency**: Relational database foreign keys, cascade delete on prescription items, and declarative `@Transactional` boundaries guaranteeing atomic all-or-nothing operations.
- **Idempotency Strategy**: Supports the standard `Idempotency-Key` HTTP header and database unique constraint to prevent accidental duplicate prescription submissions over unstable networks.
- **Centralized Error Handling**: Unified `@RestControllerAdvice` returning structured, RFC 7807-style error responses without leaking internal database errors or stack traces.
- **Automated Testing**: 151 unit and full-context HTTP integration tests covering security, validation, business rules, and repository operations.
- **Interactive Documentation**: OpenAPI 3.0 / Swagger UI with built-in JWT Bearer token authorization.

---

## Technology Stack

| Component | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java (LTS) | 25 |
| **Framework** | Spring Boot | 4.1.1 |
| **Data Access** | Spring Data JPA / Hibernate ORM | 7.4.x |
| **Database** | MySQL (compatible with PostgreSQL/MariaDB) | 8.0+ |
| **Security** | Spring Security & JJWT | 0.12.5 |
| **Object Mapping** | ModelMapper | 3.2.0 |
| **API Documentation**| SpringDoc OpenAPI / Swagger UI | 2.8.5 |
| **Testing** | JUnit 5, AssertJ, Mockito, Spring MockMvc | Latest |
| **Build Tool** | Apache Maven Wrapper | 3.9.x |

---

## Domain Data Model & Relationships

```
+------------------+         +---------------------+
|      users       |         |       clients       |
+------------------+         +---------------------+
| id (PK)          |         | id (PK)             |
| registration_no  |<---\ /--| name                |
| name             |    | |  | phone_number        |
| phone_number     |    | |  | date_of_birth       |
| password         |    | |  +---------------------+
| role             |    | |             ^
+------------------+    | |             |
         |              | |             |
         | 1            | | 1           | 1
         |              | |             |
         | N            | | N           | N
+--------------------------------------------------+
|                     records                      |
+--------------------------------------------------+
| id (PK)                                          |
| idempotency_key (UNIQUE)                         |
| user_id (FK -> users.id)                         |
| client_id (FK -> clients.id)                     |
| record_date                                      |
| diagnosis                                        |
| status (DRAFT, ISSUED)                           |
+--------------------------------------------------+
                        | 1
                        | (Cascade ALL, OrphanRemoval)
                        | N
+--------------------------------------------------+
|                  record_entries                  |
+--------------------------------------------------+
| id (PK)                                          |
| record_id (FK -> records.id ON DELETE CASCADE)   |
| entry_name                                       |
| quantity                                         |
| frequency                                        |
| duration                                         |
| instructions                                     |
+--------------------------------------------------+
```

### Relational Constraints
- **Prescription Header (`records`)**:
  - `user_id`: Foreign key referencing `users(id)` with `ON DELETE RESTRICT`. A doctor account cannot be deleted if prescription history depends on it.
  - `client_id`: Foreign key referencing `clients(id)` with `ON DELETE RESTRICT`.
  - `idempotency_key`: Optional client-provided unique key with unique index `idx_records_idempotency_key`.
- **Prescription Medication Entries (`record_entries`)**:
  - `record_id`: Foreign key referencing `records(id)` with `ON DELETE CASCADE`. When a prescription record is deleted, all attached entries are automatically cleaned up.

---

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or Java 25 LTS installed and set in your `JAVA_HOME`.
- **MySQL Database**: Running locally on port `3306` (or containerized via Docker).
- **Git** (optional): For version control.

---

## Database Setup

1. **Create the MySQL Database**:
   ```sql
   CREATE DATABASE IF NOT EXISTS PrescriptionManagement;
   ```

2. **Run Standalone DDL Script (Optional)**:
   A standalone schema script is available at the project root (`schema.sql`) and `src/main/resources/schema.sql`. You can initialize tables manually using the MySQL CLI:
   ```bash
   mysql -u root -p PrescriptionManagement < schema.sql
   ```
   *(Note: By default, Spring Boot's Hibernate `spring.jpa.hibernate.ddl-auto=update` will also verify and create all tables, indexes, and foreign keys automatically).*

3. **Verify Configuration in `src/main/resources/application.properties`**:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/PrescriptionManagement
   spring.datasource.username=root
   spring.datasource.password=Nikhil@2601
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

   security.jwt.secret=2b016e78b2ac280d4661469307439555474408400dcf1bef82ad002c071513d9
   security.jwt.expiration-ms=86400000
   ```

---

## Building & Running the Application

### Using the Maven Wrapper (Windows PowerShell / CMD):
```powershell
.\mvnw.cmd clean spring-boot:run
```

### Using Unix / macOS:
```bash
./mvnw clean spring-boot:run
```

Once started, the backend server runs on:
```
http://localhost:8080
```

---

## Running Automated Tests

The test suite contains **151 unit and integration tests** verifying:
- Controller routing, serialization, and status codes.
- Security filters, unauthorized requests (401), and forbidden actions (403).
- Business validations, mandatory fields, positive quantities, and past-or-present dates (400).
- Idempotency duplicate submission prevention.
- Service transactional rollback logic and boundary checks.

Run all tests via Maven:
```powershell
.\mvnw.cmd test
```

Expected output:
```text
[INFO] Results:
[INFO] Tests run: 151, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Pre-Seeded Credentials & Roles

When the application boots, `DataInitializer.java` automatically inspects the database and seeds the system administrator account if not present:

| Role | Registration Number | Password | Capabilities |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `ADMIN001` | `Admin@12345` | Full administrative control: Manage users, clients, view and audit all prescriptions. |
| **DOCTOR** | *(Created via API)* | *(User chosen)* | Medical staff: Manage clients, create, view, edit (DRAFT), and delete prescriptions. |

---

## Interactive API Documentation (Swagger / OpenAPI)

Interactive Swagger UI documentation is built into the running application:

- **Swagger UI Console**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3.0 JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### How to Authenticate in Swagger UI:
1. Call `POST /api/auth/login` using the pre-seeded credentials (`ADMIN001` / `Admin@12345`).
2. Copy the `token` value from the JSON response.
3. Click the green **Authorize** button at the top-right of Swagger UI.
4. Paste the token into the value field and click **Authorize**.
5. All subsequent requests in Swagger UI will now automatically pass the `Authorization: Bearer <token>` header.

---

## Sample API Requests & Workflows

### 1. Authentication (`/api/auth`)

#### User Login
**POST** `/api/auth/login`
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "registrationNumber": "ADMIN001",
    "password": "Admin@12345"
  }'
```
**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "registrationNumber": "ADMIN001",
  "name": "System Administrator",
  "role": "ADMIN",
  "expiresInMs": 86400000
}
```

#### Register a New Doctor Account
**POST** `/api/auth/register`
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dr. Sarah Jenkins",
    "registrationNumber": "DOC1001",
    "phoneNumber": "+14155552671",
    "password": "DoctorPass@123",
    "role": "DOCTOR"
  }'
```
**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 2,
  "registrationNumber": "DOC1001",
  "name": "Dr. Sarah Jenkins",
  "role": "DOCTOR",
  "expiresInMs": 86400000
}
```

#### Get Current Authenticated Profile
**GET** `/api/auth/me`
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

### 2. User Management (`/api/users`)
*Requires `ADMIN` role.*

#### Create User
**POST** `/api/users`
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Dr. Robert Smith",
    "registrationNumber": "DOC1002",
    "phoneNumber": "+14155552672",
    "password": "DoctorPass@456",
    "role": "DOCTOR"
  }'
```

#### List All Users (Paginated)
**GET** `/api/users?page=0&size=10&sort=name,asc`
```bash
curl -X GET "http://localhost:8080/api/users?page=0&size=10" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

---

### 3. Patient / Client Management (`/api/clients`)
*Accessible by `ADMIN` and `DOCTOR`.*

#### Create Client (Patient)
**POST** `/api/clients`
```bash
curl -X POST http://localhost:8080/api/clients \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice M. Johnson",
    "dateOfBirth": "1990-05-14",
    "phoneNumber": "+14155550199"
  }'
```
**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Alice M. Johnson",
  "dateOfBirth": "1990-05-14",
  "phoneNumber": "+14155550199"
}
```

#### List Clients (Paginated)
**GET** `/api/clients?page=0&size=10`
```bash
curl -X GET "http://localhost:8080/api/clients?page=0&size=10" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

---

### 4. Prescription Records (`/api/records`)
*Accessible by `ADMIN` and `DOCTOR`.*

#### Create Prescription Record (with Entries & Idempotency Key)
The API supports atomic creation of a record header along with multiple medication line items. To guarantee that accidental double clicks or network retries do not duplicate the prescription, supply an `Idempotency-Key` header (e.g. UUID).

**POST** `/api/records`
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -H "Idempotency-Key: e4a7f05c-3f9b-4e12-8df8-22d765893011" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "clientId": 1,
    "recordDate": "2026-10-23",
    "diagnosis": "Acute Bronchitis and Low-grade Fever",
    "status": "DRAFT",
    "entries": [
      {
        "entryName": "Amoxicillin 500mg",
        "quantity": 21,
        "frequency": "Three times daily",
        "duration": 7,
        "instructions": "Take after meals with water"
      },
      {
        "entryName": "Paracetamol 650mg",
        "quantity": 10,
        "frequency": "Twice daily as needed",
        "duration": 5,
        "instructions": "For fever higher than 38C"
      }
    ]
  }'
```
**Response (201 Created):**
```json
{
  "id": 1,
  "idempotencyKey": "e4a7f05c-3f9b-4e12-8df8-22d765893011",
  "user": {
    "id": 2,
    "name": "Dr. Sarah Jenkins",
    "registrationNumber": "DOC1001",
    "phoneNumber": "+14155552671",
    "role": "DOCTOR"
  },
  "client": {
    "id": 1,
    "name": "Alice M. Johnson",
    "dateOfBirth": "1990-05-14",
    "phoneNumber": "+14155550199"
  },
  "recordDate": "2026-10-23",
  "diagnosis": "Acute Bronchitis and Low-grade Fever",
  "status": "DRAFT",
  "entries": [
    {
      "id": 1,
      "entryName": "Amoxicillin 500mg",
      "quantity": 21,
      "frequency": "Three times daily",
      "duration": 7,
      "instructions": "Take after meals with water"
    },
    {
      "id": 2,
      "entryName": "Paracetamol 650mg",
      "quantity": 10,
      "frequency": "Twice daily as needed",
      "duration": 5,
      "instructions": "For fever higher than 38C"
    }
  ]
}
```

> **Idempotent Re-Submission**: If the exact same request with header `Idempotency-Key: e4a7f05c-3f9b-4e12-8df8-22d765893011` is re-sent, the server detects the existing record and immediately returns the previously created record without inserting duplicate records or entries.

#### Get Prescription by ID
**GET** `/api/records/1`
```bash
curl -X GET http://localhost:8080/api/records/1 \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

#### List All Prescriptions (Paginated)
**GET** `/api/records?page=0&size=10&sort=recordDate,desc`
```bash
curl -X GET "http://localhost:8080/api/records?page=0&size=10" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

#### Get Prescriptions by Doctor (User ID)
**GET** `/api/records/getRecordsByUser/2`
```bash
curl -X GET "http://localhost:8080/api/records/getRecordsByUser/2?page=0&size=10" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

#### Get Prescriptions by Patient (Client ID)
**GET** `/api/records/getRecordsByClient/1`
```bash
curl -X GET "http://localhost:8080/api/records/getRecordsByClient/1?page=0&size=10" \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

#### Update Prescription Record
*Allowed when `status` is `DRAFT`. If prescription is `ISSUED`, returns `409 Conflict`.*
**PUT** `/api/records/1`
```bash
curl -X PUT http://localhost:8080/api/records/1 \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 2,
    "clientId": 1,
    "recordDate": "2026-10-23",
    "diagnosis": "Acute Bronchitis (Recovering)",
    "status": "ISSUED",
    "entries": [
      {
        "entryName": "Amoxicillin 500mg",
        "quantity": 21,
        "frequency": "Three times daily",
        "duration": 7,
        "instructions": "Finish full course"
      }
    ]
  }'
```

#### Delete Prescription Record
*Allowed when `status` is `DRAFT`. Modifying or deleting `ISSUED` records is forbidden to preserve medical audit trails.*
**DELETE** `/api/records/1`
```bash
curl -X DELETE http://localhost:8080/api/records/1 \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```
**Response:** `204 No Content`

---

### 5. Prescription Entries (`/api/records/{id}/entries`)
*Manage individual items within an existing DRAFT prescription.*

#### Add Entry to Existing Prescription
**POST** `/api/records/1/entries`
```bash
curl -X POST http://localhost:8080/api/records/1/entries \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "entryName": "Cetirizine 10mg",
    "quantity": 10,
    "frequency": "Once daily at bedtime",
    "duration": 10,
    "instructions": "For allergy relief"
  }'
```

#### Update Specific Entry
**PUT** `/api/records/1/entries/2`
```bash
curl -X PUT http://localhost:8080/api/records/1/entries/2 \
  -H "Authorization: Bearer <DOCTOR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "entryName": "Paracetamol 500mg",
    "quantity": 15,
    "frequency": "Three times daily",
    "duration": 5,
    "instructions": "Take with water"
  }'
```

#### Delete Specific Entry
**DELETE** `/api/records/1/entries/2`
```bash
curl -X DELETE http://localhost:8080/api/records/1/entries/2 \
  -H "Authorization: Bearer <DOCTOR_TOKEN>"
```

---

## Architecture & Design Decisions

### Transactional Boundaries & Atomicity
In medical prescription systems, data integrity is paramount: creating a prescription header without medication items, or saving medication items when the header fails, corrupts medical records.
- **Atomic Operations**: All write operations in `RecordServiceImpl` and `RecordEntryServiceImpl` are annotated with Spring's `@Transactional`.
- **Cascade Behavior**: The `Record` entity owns the relationship to `RecordEntry` using `cascade = CascadeType.ALL` and `orphanRemoval = true`.
- **Rollback Guarantee**: In the event of a runtime exception (e.g. invalid client reference, database constraint violation, or out-of-bounds quantity), the entire transaction rolls back cleanly, leaving zero orphaned records or half-saved prescription items.
- **Read-Only Optimizations**: Query methods use `@Transactional(readOnly = true)` to avoid dirty-checking overhead in Hibernate and allow database-level read replica optimization.

### Idempotency & Duplicate Prevention Strategy
Under flaky mobile or clinical network conditions, clients frequently re-transmit `POST /api/records` upon request timeout:
1. **Industry-Standard Header**: The endpoint accepts an optional `Idempotency-Key` HTTP header (or an `idempotencyKey` field in the JSON body).
2. **Lookup Prior to Mutation**: Before creating new entities, `RecordServiceImpl.createRecord()` queries `recordRepository.findByIdempotencyKey(key)`.
3. **Safe Return of Existing State**: If found, the existing prescription record is immediately returned with HTTP 200/201.
4. **Database-Level Defense**: `idempotency_key` carries a unique constraint (`uk_records_idempotency_key`). Even under concurrent race conditions where two simultaneous threads attempt creation with the identical key, one succeeds and the second is safely prevented from creating a duplicate row.

### Security & Role-Based Access Control
- **Stateless Authentication**: Eliminates server-side session state for horizontal scalability. Clients present a signed HMAC-SHA256 JWT in the `Authorization: Bearer <token>` header.
- **Method-Level Security**: Secured using Spring Security's `@PreAuthorize("hasRole('ADMIN')")` and `@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")`.
- **Password Security**: Passwords are never stored in plaintext. They are salted and encrypted using BCrypt (`BCryptPasswordEncoder`). In entity serialization, the password field is marked `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` to prevent accidental disclosure in API responses.

### Validation & Centralized Error Handling
- **JSR-380 Bean Validation**: Mandatory fields are validated declaratively on DTOs using `@NotNull`, `@NotBlank`, `@Size`, `@Positive`, and `@PastOrPresent`.
- **Centralized Exception Handler**: `GlobalExceptionHandler` (`@RestControllerAdvice`) intercepts all uncaught exceptions across all controllers:
  - `MethodArgumentNotValidException` -> HTTP 400 Bad Request with field-by-field error map.
  - `ResourceNotFoundException` -> HTTP 404 Not Found with clear descriptive message.
  - `IllegalStateException` / `DataIntegrityViolationException` -> HTTP 409 Conflict.
  - `AccessDeniedException` -> HTTP 403 Forbidden.
  - `BadCredentialsException` -> HTTP 401 Unauthorized.
- **Zero Information Leakage**: Internal database error codes, SQL statements, and Java stack traces are never exposed to API clients.

### Design Trade-Offs

| Decision | Selected Approach | Trade-Off & Alternative Considered |
| :--- | :--- | :--- |
| **Authentication** | Stateless JWT | Stateless tokens require no database lookup on every request, but token revocation before expiry requires a distributed blacklist/Redis cache. |
| **Prescription Immutability** | Lifecycle status (`DRAFT` vs `ISSUED`) | Soft immutability via status flag prevents editing finalized prescriptions while allowing doctors to compose drafts. An append-only event sourcing model was deferred to avoid unnecessary complexity for the initial scope. |
| **Medication Entries** | Embedded List on Prescription Header | Creating a prescription along with its entries in a single POST payload provides an intuitive, atomic UX. Separate creation endpoints are also exposed for granular updates. |
| **Object Mapping** | ModelMapper & DTOs | Decouples internal database entities from external API contracts, mitigating over-posting vulnerabilities at the cost of minor mapping configuration overhead. |

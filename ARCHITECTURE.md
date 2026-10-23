# Prescription Management System: Architecture & Design Trade-offs Document

> **Document Purpose**: This document provides an in-depth technical explanation of the architecture, transactional model, security infrastructure, validation mechanisms, and deliberate design trade-offs made in the **Prescription Management REST API** (Strategy Point 8).

---

## Table of Contents
1. [Architectural Overview & Structural Paradigm](#1-architectural-overview--structural-paradigm)
2. [Component & Package Breakdown](#2-component--package-breakdown)
3. [Deep-Dive: Core Architectural Mechanisms](#3-deep-dive-core-architectural-mechanisms)
   - [3.1 Transaction Management & Atomic Boundaries](#31-transaction-management--atomic-boundaries)
   - [3.2 Idempotency & Duplicate Submission Prevention](#32-idempotency--duplicate-submission-prevention)
   - [3.3 Security Architecture & RBAC](#33-security-architecture--rbac)
   - [3.4 Validation Pipeline & Defensive Error Handling](#34-validation-pipeline--defensive-error-handling)
4. [Significant Design Trade-offs Analysis](#4-significant-design-trade-offs-analysis)
   - [Trade-off 1: Stateless JWT Authentication vs. Stateful Server Sessions](#trade-off-1-stateless-jwt-authentication-vs-stateful-server-sessions)
   - [Trade-off 2: Composite Prescription Payload vs. Multi-Step Normalized REST APIs](#trade-off-2-composite-prescription-payload-vs-multi-step-normalized-rest-apis)
   - [Trade-off 3: Status-Based Immutability vs. Append-Only Event Sourcing](#trade-off-3-status-based-immutability-vs-append-only-event-sourcing)
   - [Trade-off 4: Client-Driven Idempotency Keys vs. Natural Hash Fingerprinting](#trade-off-4-client-driven-idempotency-keys-vs-natural-hash-fingerprinting)
   - [Trade-off 5: ModelMapper Object Mapping vs. MapStruct / Manual Builders](#trade-off-5-modelmapper-object-mapping-vs-mapstruct--manual-builders)
   - [Trade-off 6: Cascade All / Orphan Removal vs. Independent Aggregate Roots](#trade-off-6-cascade-all--orphan-removal-vs-independent-aggregate-roots)
5. [Database & Concurrency Considerations](#5-database--concurrency-considerations)
6. [Automated Testing Strategy](#6-automated-testing-strategy)

---

## 1. Architectural Overview & Structural Paradigm

The Prescription Management backend implements a **Layered Domain Architecture** enforcing strict separation of concerns, high cohesion, and low coupling. Every inbound HTTP request traverses distinct, single-responsibility layers:

```
[ HTTP Client / Browser / Swagger UI ]
                   │
                   ▼ (1. HTTPS / JSON)
┌────────────────────────────────────────────────────────┐
│                   Security Filter Chain                │
│  - JwtAuthenticationFilter (Token Parsing & Validation) │
│  - SecurityContextHolder Population                    │
│  - CustomAuthenticationEntryPoint (401 Unauthorized)   │
│  - CustomAccessDeniedHandler (403 Forbidden)           │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼ (2. Authenticated Request)
┌────────────────────────────────────────────────────────┐
│                     Controller Layer                   │
│  - REST routing & HTTP Status mapping                  │
│  - JSR-380 Request Body Validation (@Valid)           │
│  - OpenAPI / Swagger Annotations                       │
│  - @PreAuthorize Role checks ('ADMIN', 'DOCTOR')       │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼ (3. Request DTO)
┌────────────────────────────────────────────────────────┐
│                      Service Layer                     │
│  - Declarative Transaction Management (@Transactional) │
│  - Business logic & Domain integrity rules             │
│  - Idempotency verification & duplicate rejection      │
│  - DTO <-> Entity transformation (ModelMapper)         │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼ (4. Domain Entities)
┌────────────────────────────────────────────────────────┐
│                    Repository Layer                    │
│  - Spring Data JPA abstractions & derived queries      │
│  - Paginated data retrieval (Pageable)                 │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼ (5. SQL / JDBC)
┌────────────────────────────────────────────────────────┐
│               Relational Database (MySQL)              │
│  - ACID transaction isolation (REPEATABLE READ)       │
│  - Primary keys, Foreign keys, and Unique constraints  │
│  - Clustered and Secondary Indexes                     │
└────────────────────────────────────────────────────────┘
```

---

## 2. Component & Package Breakdown

| Package | Primary Responsibility | Key Classes / Interfaces |
| :--- | :--- | :--- |
| `Controller` | HTTP endpoints, status codes, OpenAPI docs, authorization gates. | `AuthController`, `UserController`, `ClientController`, `RecordController`, `RecordEntryController` |
| `Service` & `ServiceImpl` | Transaction boundaries, business validation, idempotency checks. | `AuthService`, `UserService`, `ClientService`, `RecordService`, `RecordEntryService` |
| `Repository` | Data access layer using Spring Data JPA. | `UserRepository`, `ClientRepository`, `RecordRepository`, `RecordEntryRepository` |
| `Entity` | JPA entities representing relational database tables and foreign keys. | `User`, `Client`, `Record`, `RecordEntry`, `RecordStatus`, `Role` |
| `DTO.RequestDto` | Inbound request contracts enforcing JSR-380 input validation. | `LoginRequest`, `RegisterRequest`, `UserRequest`, `ClientRequest`, `RecordRequest`, `RecordEntryRequest` |
| `DTO.ResponseDto`| Outbound response contracts masking sensitive data (e.g., passwords). | `AuthResponse`, `UserResponse`, `ClientResponse`, `RecordResponse`, `RecordEntryResponse` |
| `Security` | JWT parsing, signature verification, custom user details, filters. | `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `CustomUserDetails`, `CustomUserDetailsService` |
| `Exception` | Centralized exception handling and standardized error JSON output. | `GlobalExceptionHandler`, `ApiErrorResponse`, `ResourceNotFoundException` |
| `Config` | Cross-cutting configurations (OpenAPI 3.0, data initializers). | `OpenApiConfig`, `DataInitializer` |

---

## 3. Deep-Dive: Core Architectural Mechanisms

### 3.1 Transaction Management & Atomic Boundaries
- **The Problem**: A prescription consists of a header (`records`) and multiple medication items (`record_entries`). If a failure occurs while inserting line items (e.g., database timeout or validation failure), leaving an orphaned record header without medications violates medical safety regulations.
- **Implementation**:
  - `RecordServiceImpl.createRecord()` and `updateRecord()` are annotated with `@Transactional`.
  - Hibernate cascades (`cascade = CascadeType.ALL`, `orphanRemoval = true`) bind the child entries' lifecycle to the parent `Record`.
  - When an unhandled runtime exception or validation failure occurs, Spring's `TransactionInterceptor` triggers a rollback at the JDBC driver level (`ROLLBACK WORK`), ensuring **all-or-nothing atomicity**.
  - Query methods are decorated with `@Transactional(readOnly = true)`, disabling Hibernate dirty-checking and reducing CPU/memory overhead.

### 3.2 Idempotency & Duplicate Submission Prevention
- **The Problem**: Mobile networks and hospital Wi-Fi often experience packet drops. If an HTTP response is lost, clients automatically retry `POST /api/records`, potentially creating duplicate prescriptions with identical dosages.
- **Implementation**:
  1. The API supports the standard `Idempotency-Key` header (e.g., client UUID).
  2. Prior to persistence, `RecordServiceImpl` invokes `recordRepository.findByIdempotencyKey(key)`.
  3. If a match is found, the existing prescription is returned immediately without re-executing entity persistence.
  4. At the database level, `idempotency_key VARCHAR(120) UNIQUE` provides a second line of defense against concurrent race conditions.

### 3.3 Security Architecture & RBAC
- **Authentication**: Stateless HMAC-SHA256 JWT tokens. `JwtAuthenticationFilter` intercepts requests before Spring's `UsernamePasswordAuthenticationFilter`, decodes claims, verifies cryptographic signatures, and injects `UsernamePasswordAuthenticationToken` into the thread-local `SecurityContextHolder`.
- **Role Hierarchy & Method Security**:
  - `ADMIN`: Full administrative control over users, clients, and medical audits.
  - `DOCTOR`: Operational privileges to manage clients, write prescriptions, and edit drafts.
  - Secured via `@PreAuthorize("hasRole('ADMIN')")` and `@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")`.
- **Data Protection**: User passwords are encrypted with BCrypt (strength 10). On the `User` entity, `password` is annotated with `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` to guarantee that hashes are never exposed through API responses or ModelMapper serialization.

### 3.4 Validation Pipeline & Defensive Error Handling
- **JSR-380 Annotations**: Validated at the controller boundary before hitting service logic:
  - `@NotBlank`, `@Size(max = 120)` on names and registrations.
  - `@Pattern(regexp = "^\\+?[0-9]{7,15}$")` on telephone numbers.
  - `@Positive` on medication quantities and durations.
  - `@PastOrPresent` on prescription dates and birthdates.
  - `@NotEmpty` on prescription entry lists.
- **Centralized Handler (`GlobalExceptionHandler`)**:
  - Catches `MethodArgumentNotValidException`, converting validation failures into structured HTTP 400 maps (`fieldName: errorMessage`).
  - Catches `ResourceNotFoundException`, mapping to HTTP 404 with contextual detail.
  - Catches `IllegalStateException` (e.g. attempting to edit `ISSUED` records), mapping to HTTP 409 Conflict.
  - Sanitizes unexpected exceptions into HTTP 500 without leaking stack traces or SQL snippets.

---

## 4. Significant Design Trade-offs Analysis

### Trade-off 1: Stateless JWT Authentication vs. Stateful Server Sessions

| Dimension | Chosen Approach: Stateless JWT | Alternative: Stateful HTTP Sessions (Redis/HttpSession) |
| :--- | :--- | :--- |
| **Scalability** | **High**: Zero server memory overhead; requests can be routed to any cluster replica without sticky sessions. | **Medium**: Requires shared session store (e.g., Redis cluster) or sticky load balancer sessions. |
| **Revocation** | **Moderate**: Tokens cannot be revoked before expiration unless a centralized blacklist is maintained. | **High**: Instant token/session invalidation by deleting the session record in Redis. |
| **Latency** | **Sub-millisecond**: Cryptographic signature validation happens in-memory without database queries. | **Moderate**: Requires a network roundtrip to Redis/DB on every authenticated request. |
| **Decision Rationale** | For a high-throughput REST API with distributed deployment targets, stateless JWT was selected. Token lifespan is kept reasonable (24 hours), and token claims carry `userId`, `role`, and `name` to eliminate repeated user lookups. |

---

### Trade-off 2: Composite Prescription Payload vs. Multi-Step Normalized REST APIs

| Dimension | Chosen Approach: Composite Creation (`Record` + `entries`) | Alternative: Multi-Step REST Workflow (Create Record, then Add Entries) |
| :--- | :--- | :--- |
| **Atomicity** | **Guaranteed**: Either the prescription and all medications save together, or nothing is saved. | **Risky**: If the client disconnects after creating the header, an empty, unusable prescription record remains. |
| **Network Roundtrips** | **1 Roundtrip**: Single HTTP POST payload. | **N+1 Roundtrips**: 1 POST for record header + N POSTs for individual medications. |
| **Payload Complexity**| **Higher**: Nested JSON structure requiring cascading validation. | **Lower**: Simple flat JSON structures per request. |
| **Decision Rationale** | Medical prescriptions are intrinsically composite documents. A doctor does not write an empty prescription and walk away. Creating the record header and items atomically in one transaction guarantees data integrity while minimizing mobile network latency. Separate entry endpoints are retained for granular draft modifications. |

---

### Trade-off 3: Status-Based Immutability vs. Append-Only Event Sourcing

| Dimension | Chosen Approach: Lifecycle Status (`DRAFT` vs `ISSUED`) | Alternative: Full Event Sourcing (CQRS + Event Store) |
| :--- | :--- | :--- |
| **Audit Compliance** | **High**: Business logic strictly blocks `PUT` and `DELETE` on `ISSUED` records (409 Conflict). | **Maximum**: Complete immutable history of every state transition and draft revision. |
| **System Complexity**| **Low/Manageable**: Standard relational schema with simple status checks in service methods. | **High**: Requires event store, snapshotting, projectors, and eventual consistency handling. |
| **Query Performance**| **Direct**: Standard indexed SQL queries with pagination. | **Indirect**: Requires read projections / denormalized view tables. |
| **Decision Rationale** | Enforcing immutability via status flags satisfies healthcare compliance requirements (preventing retroactive alteration of issued prescriptions) without introducing the massive architectural overhead and eventual consistency latency of Event Sourcing. |

---

### Trade-off 4: Client-Driven Idempotency Keys vs. Natural Hash Fingerprinting

| Dimension | Chosen Approach: Client-Supplied `Idempotency-Key` Header | Alternative: Server-Side Natural Hash (`userId + clientId + date + diagnosis`) |
| :--- | :--- | :--- |
| **Flexibility** | **High**: Client explicitly controls retry scope. A doctor intentionally creating two similar prescriptions for twins on the same day is not blocked. | **Low**: A natural hash could produce false positives, rejecting legitimate duplicate diagnoses on the same day. |
| **Implementation** | **Standard**: Follows Stripe, AWS, and IETF `Idempotency-Key` draft specifications. | **Custom**: Fragile hashing algorithm requiring normalization of diagnosis whitespace and timestamps. |
| **Client Responsibility** | Requires API clients to generate and transmit unique keys (e.g. UUID v4). | Transparent to the client. |
| **Decision Rationale** | Natural key hashing is prone to false-positive rejections in healthcare workflows where patients frequently revisit doctors with recurring symptoms. Client-driven idempotency keys give explicit, deterministic control to client applications and retry libraries. |

---

### Trade-off 5: ModelMapper Object Mapping vs. MapStruct / Manual Builders

| Dimension | Chosen Approach: ModelMapper | Alternative: MapStruct (Annotation Processor) |
| :--- | :--- | :--- |
| **Boilerplate** | **Minimal**: Automatic convention-based mapping without generating code or hand-crafting builders. | **Moderate**: Requires interface declarations and build-time code generation. |
| **Performance** | **Reflective**: Minor reflection overhead on first-time mapping resolution. | **Zero Overhead**: Generates plain Java bytecode at compile-time. |
| **Configuration** | Dynamic runtime matching. | Compile-time verification of field names and types. |
| **Decision Rationale** | For the scope of this domain model, ModelMapper provides rapid, clean decoupling between entities and DTOs with minimal boilerplate. To mitigate reflection overhead, the `ModelMapper` instance is configured as a Spring singleton bean. |

---

### Trade-off 6: Cascade All / Orphan Removal vs. Independent Aggregate Roots

| Dimension | Chosen Approach: `CascadeType.ALL` + `orphanRemoval` | Alternative: Decoupled Independent Entities |
| :--- | :--- | :--- |
| **Consistency** | **High**: JPA automatically syncs in-memory collection updates with SQL `INSERT`/`UPDATE`/`DELETE`. | **Manual**: Developer must manually invoke `recordEntryRepository.delete()` for deleted items. |
| **Performance** | Clean batch operations managed by Hibernate session. | Fine-grained individual query control. |
| **Risk** | Accidental clearing of collection deletes database rows. | Risk of lingering orphan rows if developer forgets cleanup logic. |
| **Decision Rationale** | `RecordEntry` has no independent existence outside of a `Record`. Modeling `Record` as the aggregate root with `orphanRemoval = true` accurately represents the domain: deleting or updating a prescription naturally reclaims its entries. |

---

## 5. Database & Concurrency Considerations

### 5.1 Relational Integrity & Deletion Rules
- **Foreign Key to Users (`user_id`)**: `ON DELETE RESTRICT`. Ensures doctor profiles cannot be deleted if historical prescription records reference them, maintaining regulatory auditability.
- **Foreign Key to Clients (`client_id`)**: `ON DELETE RESTRICT`. Protects patient medical histories from accidental cascade deletion.
- **Foreign Key to Entries (`record_id`)**: `ON DELETE CASCADE`. Guarantees clean cleanup of medication entries when a draft prescription is discarded.

### 5.2 Performance Indexing Strategy
To prevent full table scans as the prescription dataset grows into millions of records:
1. `idx_records_user_id`: Optimizes doctor prescription history lookups (`/getRecordsByUser/{userId}`).
2. `idx_records_client_id`: Optimizes patient medical record retrieval (`/getRecordsByClient/{clientId}`).
3. `idx_records_record_date`: Speeds up date-range filtering, audit reports, and default chronological sorting.
4. `idx_records_idempotency_key` (UNIQUE): Provides instantaneous $O(1)$ B-Tree lookups for duplicate check operations.
5. `idx_entries_record_id`: Optimizes child entry joins during prescription hydration.

---

## 6. Automated Testing Strategy

A reliable medical system demands comprehensive automated test coverage:

```
Total Test Suite: 151 Automated Tests (Zero Failures)
├── Unit Tests (Mockito & AssertJ)
│   ├── AuthServiceImplTest: Credential verification, token issuance, registration conflicts
│   ├── UserServiceImplTest: Validation, password encryption, pagination, role assignment
│   ├── ClientServiceImplTest: Client lifecycle, date validation, not-found exceptions
│   ├── RecordServiceImplTest: Idempotency deduplication, status checks, transactional cascades
│   └── RecordEntryServiceImplTest: Medication limits, duration checks, cascade cleanup
└── Integration Tests (@SpringBootTest & MockMvc)
    ├── SecurityIntegrationTest: JWT filter chain, public vs protected routes, 401/403 responses
    ├── AuthControllerIntegrationTest: Login, registration, token structure
    ├── UserControllerIntegrationTest: Role-based admin access control
    ├── ClientControllerIntegrationTest: Patient record management
    ├── RecordControllerIntegrationTest: Atomic prescription creation, duplicate header prevention
    ├── RecordEntryControllerIntegrationTest: Nested medication item management
    └── OpenApiIntegrationTest: OpenAPI 3.0 contract and Swagger UI availability
```

### Verification Command:
```bash
.\mvnw.cmd test
```
All 151 tests run and pass synchronously with zero warnings or errors.

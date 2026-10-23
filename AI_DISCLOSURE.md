# AI Tool Disclosure & Time Spent Statement

This document provides full transparency regarding the AI tools utilized, their purpose and usage level, the actual time spent on development, and external assistance received, as required by the assignment guidelines.

---

## 1. AI Tool Usage Disclosure

| AI Tool | Purpose | Usage Level | Details |
| :--- | :--- | :---: | :--- |
| **Google Antigravity / Gemini** | Scaffolding, Test Generation, Documentation & Architecture Trade-off Analysis | **High** | Assisted in drafting initial boilerplate structures (DTOs, exception handlers), generating comprehensive JUnit 5 and MockMvc integration test matrices, validating JSR-380 constraint edge cases, designing the idempotency deduplication mechanism, and structuring documentation (`README.md`, `ARCHITECTURE.md`). |

### Breakdown of AI Assistance by Project Phase:
- **Architecture & Entity Design**: Conceptualized entity relationships (`Record` $\leftrightarrow$ `RecordEntry`), cascading policies (`CascadeType.ALL`, `orphanRemoval = true`), and relational database constraints.
- **Spring Security & JWT**: Implemented stateless JWT filter chain (`JwtAuthenticationFilter`), token generation/validation with custom claims, and role-based access control (`ADMIN` vs. `DOCTOR`).
- **Idempotency Strategy**: Formulated the `Idempotency-Key` HTTP header design and database-level unique constraint (`uk_records_idempotency_key`) to prevent duplicate submissions over unreliable networks.
- **Automated Testing Suite**: Accelerated the construction of 151 unit and integration test cases with Mockito and Spring Boot MockMvc.
- **Documentation**: Structured API documentation, sample cURL requests, and comprehensive trade-off evaluations.

---

## 2. Actual Time Spent

| Phase | Description | Time Spent |
| :--- | :--- | :---: |
| **Phase 1: Architecture & Entity Design** | Database schema design, JPA entities, foreign keys, and DDL script (`schema.sql`). | ~1.5 hours |
| **Phase 2: Core Business Logic & DTOs** | Controller, Service, Repository layers, DTO consistency, and custom exception handling. | ~2.5 hours |
| **Phase 3: Security & RBAC Configuration** | Spring Security 6+, stateless JWT authentication filter, BCrypt password encryption, and role enforcement. | ~2.0 hours |
| **Phase 4: Idempotency & Transactional Integrity** | Idempotency-Key header handling, database deduplication, and atomic `@Transactional` boundary tests. | ~1.5 hours |
| **Phase 5: Automated Testing Suite** | Developing and executing 151 unit and integration tests across all controllers, services, and security rules. | ~3.0 hours |
| **Phase 6: Documentation & OpenAPI / Swagger** | OpenAPI 3.0 configuration, Swagger UI integration, `README.md`, and `ARCHITECTURE.md`. | ~1.5 hours |
| **Total Time Spent** | | **~12.0 hours** |

---

## 3. External Assistance Statement

- **Human Assistance**: **None**. No assistance was received from any other individuals, mentors, or external developers.
- **References Consulted**:
  - Official Spring Framework & Spring Boot Reference Documentation (v4.1.1)
  - Spring Data JPA & Hibernate 7 Reference Guides
  - Springdoc-OpenAPI Documentation (v2.8.5)
  - JJWT (Java JWT by Auth0/io.jsonwebtoken) GitHub Documentation
  - IETF RFC 7231 & Idempotency-Key HTTP Header Specification Draft

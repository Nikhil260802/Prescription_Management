# AI Tool Disclosure & External Assistance Statement

This document formally declares the AI tools utilized, the extent of external assistance, and the actual time spent developing the **Prescription Management REST API**, in compliance with the assessment guidelines.

---

## 1. AI Tools Disclosure

| AI Tool / Assistant | Purpose & Scope of Usage | Usage Level |
| :--- | :--- | :---: |
| **OpenAI ChatGPT** | Initial requirements breakdown, domain entity conceptualization, early brainstorming of REST endpoints, and architectural structuring. | **Medium** |
| **Google Antigravity (Gemini)** | Pair-programming assistant: Codebase implementation, DTO separation, Spring Security stateless JWT configuration, OpenAPI 3.0 / Swagger UI setup, Idempotency-Key duplicate prevention strategy, 151 comprehensive unit and integration tests, database schema scripting (`schema.sql`), and documentation generation (`README.md`, `ARCHITECTURE.md`). | **High** |

### Detailed Breakdown of AI Usage

1. **ChatGPT**:
   - Analyzed the assessment prompt, identified key domain entities (`User`, `Client`, `Record`, `RecordEntry`).
   - Planned the initial package structure (`Controller`, `Service`, `Repository`, `DTO`, `Exception`).
   - Provided quick syntax lookups and configuration references.

2. **Google Antigravity (Gemini)**:
   - Guided end-to-end implementation of the layered architecture with Java 25 LTS and Spring Boot 4.1.1.
   - Refactored controllers to enforce strict DTO separation (`*Request` and `*Response` DTOs) instead of exposing database entities directly.
   - Designed and integrated stateless JWT authentication with role-based access control (`ADMIN` vs. `DOCTOR`).
   - Implemented the idempotency strategy (`Idempotency-Key` header and database unique constraint).
   - Authored the automated test suite (151 unit and MockMvc integration tests) ensuring 100% pass rate.
   - Authored technical documentation (`README.md`, `ARCHITECTURE.md`, `schema.sql`).

---

## 2. Actual Time Spent

| Phase / Activity | Tool / Resource | Time Spent |
| :--- | :--- | :---: |
| **Requirements Analysis & Architecture Planning** | ChatGPT & Personal Planning | ~4.0 hours |
| **Domain Model, JPA Entities & DB Schema Design** | Google Antigravity & Personal Coding | ~1.5 hours |
| **Service Layer, Business Logic & DTO Refactoring** | Google Antigravity & Personal Coding | ~2.5 hours |
| **Spring Security & Stateless JWT Integration** | Google Antigravity & Personal Coding | ~2.0 hours |
| **Idempotency Strategy & OpenAPI 3.0 Swagger Setup** | Google Antigravity & Personal Coding | ~1.5 hours |
| **Automated Testing Suite (151 Unit & Integration Tests)** | Google Antigravity & Personal Verification | ~2.5 hours |
| **Documentation (`README.md`, `ARCHITECTURE.md`, `schema.sql`)**| Google Antigravity & Personal Review | ~1.5 hours |
| **Total Time Spent** | | **~15.5 hours** |

---

## 3. External Assistance Statement

- **Human Assistance**: **None**. No code, guidance, or review was provided by external individuals, mentors, or colleagues.
- **Third-Party Libraries & Documentation**: Official Spring Boot, Hibernate ORM, and SpringDoc OpenAPI reference documentation were referenced.
- **AI Tool Assistance**: AI tools (ChatGPT and Google Antigravity/Gemini) were utilized strictly within the permitted guidelines as disclosed in Section 1. All final code, test results, and architectural decisions have been verified and validated.

# Movie Ticketing Platform

A backend movie-ticketing platform for multiple cities, theaters, shows, and seat-level booking.

The assignment asks the platform to support time-bound seat holds, pricing tiers and discount codes, payments, booking confirmation, cancellations and refunds, concurrent attempts to book the same seat, and non-blocking confirmation and reminder notifications. It defines two roles: administrators and customers.

## Project status

The project is currently in the design phase. No application code, build configuration, database schema, API contract, or tests have been implemented.

Design decisions will be made with the user through explicit, step-by-step iterations. This README records only confirmed requirements and decisions.

## Confirmed technical constraints

- Java 21
- Spring Boot
- Maven
- PostgreSQL
- Flyway
- Monolithic application architecture

No further architecture or implementation design has been selected yet.

## Scope

### In scope from the assignment

- REST APIs covering the core flows.
- Persistence to a database.
- Basic role-based access control for administrators and customers.
- Input validation and error handling.
- Unit and integration tests for core flows.
- Multiple cities and theaters.
- Multiple shows per theater.
- Seat-level selection and booking.
- Time-bound seat holds that expire automatically.
- Regular, premium, and weekend pricing tiers.
- Discount codes.
- Payments and booking confirmation.
- Cancellation and refunds under configurable policies.
- Correct handling of concurrent attempts to book the same seat.
- Confirmation and reminder notifications that do not block booking.
- Administrator management of cities, theaters, shows, seat layouts, pricing tiers, and refund policies.
- Customer show browsing, booking, cancellation, and booking history.

### Out of scope from the assignment

- UI or frontend.
- Deployment, containerization, or CI/CD.
- Distributed systems or microservices.
- Advanced authentication such as OAuth, SSO, or MFA.
- Production-grade observability, monitoring, or alerting.

## Local setup

Local setup is not available yet because the Maven/Spring Boot project has not been initialized.

The confirmed prerequisites will be:

- JDK 21
- Maven
- PostgreSQL

Exact versions, database configuration, environment variables, and startup commands will be documented after those decisions are made and the project scaffold exists.

## Running tests

There are no tests or Maven build configuration yet. Test commands and prerequisites will be documented when the testing approach and project scaffold are agreed and added.

## Assumptions

No product or technical assumptions have been approved yet. Confirmed assumptions will be added here during the design iterations.

## API documentation

The API contract has not been designed yet. Endpoints, request and response models, authentication behavior, errors, pagination, and idempotency will be documented after they are discussed and confirmed.

## Development records

- Agent instructions: [`Docs/AGENTS.md`](Docs/AGENTS.md)
- Prompt history: [`Docs/Prompts.md`](Docs/Prompts.md)

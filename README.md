# Movie Ticketing Platform

A Java/Spring Boot backend for a movie-ticketing platform covering multiple cities, theaters, shows, and seat-level booking.

The repository is being developed one capability at a time as a monolith. Identity, JWT authentication, the public city catalogue, and the first theatre-administration slice are implemented. Movies, shows, booking, pricing, payments, refunds, and notifications remain future work.

## Implemented scope

The current implementation provides:

- Public customer signup.
- Public theatre-administrator signup.
- One shared email/password sign-in endpoint.
- One `user_account` table for both roles.
- BCrypt password hashing in the account table.
- JWT bearer access tokens containing the account ID and role.
- Stateless bearer-token authentication.
- `CUSTOMER` and `THEATRE_ADMIN` role authorities.
- A current-account endpoint.
- Validation and `application/problem+json` error responses.
- Flyway-managed PostgreSQL schema.
- Unit and PostgreSQL Testcontainers integration tests.
- Replaceable authentication-provider and token-issuer boundaries.
- A public, read-only catalogue of 50 Flyway-seeded Indian cities.
- Theatre creation and owner-scoped theatre listing for `THEATRE_ADMIN` accounts.
- Auditorium creation and owner-scoped auditorium listing.
- Atomic physical-seat row creation using row label, first seat number, seat count, and tier.
- Owner-scoped physical-seat listing with response-only derived seat labels.
- Database constraints and transactions preventing duplicate physical-seat coordinates, including concurrent row requests.

Theatre, auditorium, and physical-seat APIs are intentionally create/list-only. Updates, deletion, deactivation, accessibility attributes, and physical-seat pricing are not implemented. Booking concurrency will be designed around future show-specific seat inventory rather than the physical-seat layout.

## Technology

- Java 21
- Spring Boot 3.5.16
- Maven
- PostgreSQL
- Flyway
- Spring Data JPA
- Spring Security and OAuth2 Resource Server JWT support
- JUnit 5, AssertJ, MockMvc, and Testcontainers

## Project structure

```text
src/main/java/com/dmg/movieticketing/
|- MovieTicketingApplication.java
|- identity/
|  |- api/             HTTP contracts and controller
|  |- application/     Signup, sign-in, and account use cases
|  |- config/          JWT and Spring Security configuration
|  |- domain/          Account model, roles, status, and repository
|  `- security/        Authentication provider and JWT adapters
|- city/
|  |- api/             Public city HTTP contracts
|  |- application/     Read-only catalogue use cases
|  `- domain/          City model and repository
|- theatre/
|  |- api/             Theatre, auditorium, and physical-seat HTTP contracts
|  |- application/     Owner-scoped create/list use cases
|  `- domain/          Theatre hierarchy models and repositories
`- shared/api/         Common problem response handling

src/main/resources/
|- application.yml
`- db/migration/       Flyway migrations
```

## Local setup

### Prerequisites

- JDK 21
- Maven 3.6.3 or newer
- PostgreSQL 16 or newer
- A Docker-compatible runtime when running the PostgreSQL integration tests

### Create the local database

Run as a PostgreSQL administrator:

```sql
CREATE USER movie_ticketing WITH PASSWORD 'movie_ticketing';
CREATE DATABASE movie_ticketing OWNER movie_ticketing;
```

### Configure the application

```bash
export DB_URL=jdbc:postgresql://localhost:5432/movie_ticketing
export DB_USERNAME=movie_ticketing
export DB_PASSWORD=movie_ticketing
export JWT_SECRET="$(openssl rand -base64 32)"
```

`JWT_SECRET` must be valid Base64 that decodes to at least 32 random bytes. It is required at startup and must not be committed.

Default JWT settings are:

- Issuer: `movie-ticketing-platform`
- Audience: `movie-ticketing-api`
- Access-token lifetime: 15 minutes
- Clock skew: 30 seconds
- Signing algorithm: HS256

Start the application:

```bash
mvn spring-boot:run
```

Flyway applies the database migration and Hibernate validates the resulting schema. The API is available under `http://localhost:8080/api/v1`.

## Running tests

```bash
mvn test
```

Unit tests run without external services. Identity and theatre-management integration tests use PostgreSQL 16 through Testcontainers and require an active Docker-compatible runtime. When no compatible runtime is available, those tests are reported as skipped rather than using a different database engine.

Run the complete build lifecycle with:

```bash
mvn verify
```

## Identity API

All request and response bodies use `application/json` unless noted otherwise.

### Customer signup

`POST /api/v1/auth/customers/signup`

```json
{
  "email": "customer@example.com",
  "password": "customer-password"
}
```

Returns `201 Created` with the created account and role `CUSTOMER`.

### Theatre-administrator signup

`POST /api/v1/auth/admins/signup`

```json
{
  "email": "admin@example.com",
  "password": "administrator-password"
}
```

Returns `201 Created` with the created account and role `THEATRE_ADMIN`. The role is selected by the endpoint; signup requests never contain a role field.

### Sign in

`POST /api/v1/auth/signin`

```json
{
  "email": "customer@example.com",
  "password": "customer-password"
}
```

Successful response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900,
  "account": {
    "id": "1ea8451c-e0c8-4df4-8f9c-b6dc34b43250",
    "role": "CUSTOMER"
  }
}
```

The signed JWT uses `sub` for the account UUID and includes `role` and `amr` claims. Unknown email, incorrect password, and unavailable accounts all return the same `401 INVALID_CREDENTIALS` response.

### Current account

`GET /api/v1/auth/me`

Supply `Authorization: Bearer <access-token>`. The endpoint reads and returns the current account record. Missing, malformed, invalid, or expired tokens return `401`.

### Error format

Errors use `application/problem+json`:

```json
{
  "type": "urn:movie-ticketing:problem:validation-failed",
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/v1/auth/customers/signup",
  "code": "VALIDATION_FAILED",
  "violations": [
    {
      "field": "password",
      "code": "PASSWORD_SIZE",
      "message": "size must be between 12 and 128"
    }
  ]
}
```

Important identity error codes include `EMAIL_ALREADY_REGISTERED`, `INVALID_CREDENTIALS`, `INVALID_TOKEN`, `TOKEN_EXPIRED`, and `FORBIDDEN`.

## City API

City APIs are public and read-only. They can be called anonymously or by either account role.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/cities` | List all 50 cities alphabetically. |
| `GET` | `/api/v1/cities/{cityId}` | Read one city. |

City response:

```json
{
  "id": 7,
  "name": "Ahmedabad",
  "stateOrUt": "Gujarat"
}
```

## Theatre administration API

All endpoints below require `Authorization: Bearer <access-token>` for an account with role `THEATRE_ADMIN`. The owner account ID comes from the verified JWT and is never accepted from a request. Another administrator's hierarchy is treated as not found.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/theatres` | Create an owned theatre using a seeded `cityId`. |
| `GET` | `/api/v1/theatres?page=0&size=20` | List only the caller's theatres. |
| `POST` | `/api/v1/theatres/{theatreId}/auditoriums` | Create an auditorium in an owned theatre. |
| `GET` | `/api/v1/theatres/{theatreId}/auditoriums` | List auditoriums in an owned theatre. |
| `POST` | `/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seat-rows` | Atomically create consecutive physical seats. |
| `GET` | `/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/seats` | List physical seats in an owned auditorium. |

Create a theatre:

```json
{
  "cityId": 7,
  "name": "Central Cinema",
  "addressLine1": "14 River Road",
  "addressLine2": "Navrangpura",
  "postalCode": "380009"
}
```

Create a physical-seat row:

```json
{
  "rowLabel": "A",
  "firstSeatNumber": 1,
  "seatCount": 20,
  "tier": "PREMIUM"
}
```

The request creates `A1` through `A20` in one transaction. Only `rowLabel`, `seatNumber`, and `tier` are stored. `seatLabel`, such as `A12`, is generated in API responses. Supported tiers are `REGULAR` and `PREMIUM`; accessibility and show pricing are separate future concerns.

Important theatre-management error codes include `CITY_NOT_FOUND`, `THEATRE_NOT_FOUND`, `AUDITORIUM_NOT_FOUND`, `THEATRE_NAME_CONFLICT`, `AUDITORIUM_NAME_CONFLICT`, and `SEAT_CONFLICT`.

## Confirmed identity decisions

- Email is trimmed and lowercased for login and uniqueness. Provider-specific transformations are not applied.
- The same normalized email cannot register through both signup endpoints.
- Each account has exactly one role: `CUSTOMER` or `THEATRE_ADMIN`.
- Passwords must contain 12-128 characters and no more than 72 UTF-8 bytes, preventing BCrypt truncation.
- Signup returns account metadata; users sign in separately.
- Refresh tokens, logout state, email verification, password reset, and immediate token revocation are out of scope.
- Disabling an account prevents new sign-ins, but an existing access token remains usable until its 15-minute expiry.
- JWTs contain no email address or password data.
- `/auth/me` reads the current account from PostgreSQL; ordinary JWT authentication remains stateless.

## Assignment scope not yet implemented

- Theatre, auditorium, and physical-seat updates, deactivation, and deletion.
- Accessibility attributes and visual seat-map editing.
- Movie and show management.
- Show discovery and seat availability.
- Time-bound seat holds and concurrency-safe booking.
- Show-specific pricing rules and discount codes.
- Payments and booking confirmation.
- Cancellation and configurable refunds.
- Booking history.
- Non-blocking confirmation and reminder notifications.

The assignment continues to exclude a frontend, deployment/containerization, CI/CD, microservices, advanced authentication, and production-grade monitoring.

## Documentation

- [Agent instructions](Docs/AGENTS.md)
- [Development prompts](Docs/Prompts.md)
- [Identity and authentication design](Docs/designs/identity-authentication-design.md)
- [Theatre administration design](Docs/designs/theatre-admin-management-design.md)
- [Postman API collection](Docs/collection/movie-ticketing-platform.postman_collection.json)

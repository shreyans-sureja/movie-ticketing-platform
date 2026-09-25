# Movie Ticketing Platform

A Java/Spring Boot backend for a movie-ticketing platform covering multiple cities, theaters, shows, and seat-level booking.

The repository is being developed one capability at a time as a monolith. Identity, JWT authentication, the public city catalogue, theatre administration, the movie catalogue, show scheduling, show discovery, and show-specific seat inventory are implemented. Holds, bookings, payments, refunds, and notifications remain future work.

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
- Minimal global movie creation by theatre administrators with public search and detail APIs.
- Owner-checked show scheduling with final per-tier `BigDecimal` prices and ISO 4217 currency.
- Transactional show-seat snapshot generation from physical auditorium seats.
- Auditorium-row locking that prevents concurrent overlapping shows in the same auditorium while allowing independent auditoriums to schedule concurrently.
- Public upcoming-show search by city, movie, and an `Asia/Kolkata` customer date.
- One aggregate show-search query returns show details, minimum/maximum price, and available-seat count without per-show queries.
- Public show detail and show-seat availability APIs using `showSeatId` and response-only derived seat labels.
- UTC timestamp storage and an injected UTC `Clock` for application time.

Theatre, auditorium, and physical-seat APIs are intentionally create/list-only. Updates, deletion, deactivation, accessibility attributes, and physical-seat pricing are not implemented. Future holds and bookings will operate on the implemented show-specific seat inventory rather than the physical-seat layout.

## Technology

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper 3.3.4 using Maven 3.9.11
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
|- movie/
|  |- api/             Movie command and public query contracts
|  |- application/     Catalogue creation, normalization, and search
|  `- domain/          Minimal global movie model and repository
|- show/
|  |- api/             Scheduling, discovery, and seat-availability contracts
|  |- application/     Ownership, locking, pricing, snapshots, and queries
|  `- domain/          Show, tier-price, and show-seat models
`- shared/api/         Common problem response handling

src/main/resources/
|- application.yml
`- db/migration/       Flyway migrations
```

## Local setup

### Prerequisites

- JDK 21
- PostgreSQL 16 or newer
- A Docker-compatible runtime when running the PostgreSQL integration tests

The repository includes Maven Wrapper scripts, so a separate Maven installation is not required. Use `./mvnw` on macOS/Linux or `mvnw.cmd` on Windows. The first run downloads the pinned Maven 3.9.11 distribution.

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
./mvnw spring-boot:run
```

Flyway applies the database migration and Hibernate validates the resulting schema. The API is available under `http://localhost:8080/api/v1`.

## Running tests

```bash
./mvnw test
```

Unit tests run without external services. Identity, theatre-management, and movie/show integration tests use PostgreSQL 16 through Testcontainers and require an active Docker-compatible runtime. When no compatible runtime is available, those tests are reported as skipped rather than using a different database engine.

Run the complete build lifecycle with:

```bash
./mvnw verify
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

Malformed JSON returns `400 MALFORMED_REQUEST`. Bean-validation failures, missing required query parameters, and incorrectly typed query parameters return `400 VALIDATION_FAILED` with field-level entries in `violations`. These errors use the same problem response envelope across identity, theatre, movie, and show APIs.

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

## Movie catalogue and show API

Movie reads and show reads are public. Movie creation and show scheduling require `THEATRE_ADMIN`; show scheduling also verifies that the authenticated account owns the complete theatre/auditorium path.

| Access | Method | Path | Purpose |
|---|---|---|---|
| `THEATRE_ADMIN` | `POST` | `/api/v1/movies` | Add a minimal global movie. |
| Public | `GET` | `/api/v1/movies?q=&languageCode=&page=0&size=20` | Search movies. |
| Public | `GET` | `/api/v1/movies/{movieId}` | View a movie. |
| Owning `THEATRE_ADMIN` | `POST` | `/api/v1/theatres/{theatreId}/auditoriums/{auditoriumId}/shows` | Schedule a show and generate its seat inventory. |
| Public | `GET` | `/api/v1/shows?cityId=&movieId=&date=&page=0&size=20` | Find upcoming shows. |
| Public | `GET` | `/api/v1/shows/{showId}` | View show details and prices. |
| Public | `GET` | `/api/v1/shows/{showId}/seats` | View show-seat prices and availability. |

Create a movie:

```json
{
  "title": "The Last Signal",
  "durationMinutes": 128,
  "languageCode": "hi"
}
```

Titles are trimmed and repeated whitespace is collapsed. The case-insensitive title, normalized language, and runtime form the duplicate key.

Create a show:

```json
{
  "movieId": "2baed1c9-97d7-423b-9e46-9869acc07601",
  "startsAt": "2026-10-03T18:30:00+05:30",
  "currency": "INR",
  "tierPrices": [
    {
      "tier": "REGULAR",
      "amount": 250.00
    },
    {
      "tier": "PREMIUM",
      "amount": 400.00
    }
  ]
}
```

The tier-price set must exactly match the tiers currently used by the auditorium. Monetary values use Java `BigDecimal` and PostgreSQL `NUMERIC(12,2)`, and one ISO 4217 currency is copied into the tier prices and show-seat snapshots. There is no dynamic pricing engine: an administrator controls weekday or weekend pricing by submitting final prices for each show.

Show creation accepts only a future timestamp with an explicit offset. Timestamps are normalized, stored, and returned in UTC. Customer `date` values are interpreted in `Asia/Kolkata` only for calculating UTC search bounds. Search returns upcoming shows only, so a current-date query excludes shows that have already started.

Show creation locks the target auditorium database row within the transaction, checks for overlapping half-open time ranges, and then inserts the show, prices, and all show seats atomically. The public seat response exposes `showSeatId`, not the internal physical-seat ID. `seatLabel` is generated from the snapshotted `rowLabel` and `seatNumber` and is not stored.

The public `/shows` resource is backed by the `MovieShow` JPA entity and the `movie_show` table. Show search executes one aggregate content query for the page, including minimum price, maximum price, and available-seat count, plus at most one pagination count query when needed. The number of database queries therefore does not grow with the number of shows returned.

Important catalogue/show error codes include `MOVIE_NOT_FOUND`, `MOVIE_ALREADY_EXISTS`, `SHOW_NOT_FOUND`, `SHOW_TIME_CONFLICT`, `AUDITORIUM_HAS_NO_SEATS`, and `TIER_PRICE_MISMATCH`.

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
- Time-bound seat holds and concurrency-safe booking.
- Dynamic show-pricing rules and discount codes.
- Payments and booking confirmation.
- Cancellation and configurable refunds.
- Booking history.
- Non-blocking confirmation and reminder notifications.

The assignment continues to exclude a frontend, deployment/containerization, CI/CD, microservices, advanced authentication, and production-grade monitoring.

## Documentation

- [Agent instructions](Docs/AGENTS.md)
- [Development prompts](Docs/Prompts.md)
- [Skills used](Docs/skills_used.md)
- [Identity and authentication design](Docs/designs/identity-authentication-design.md)
- [Theatre administration design](Docs/designs/theatre-admin-management-design.md)
- [Movie catalogue and show scheduling design](Docs/designs/movie-catalogue-and-show-scheduling-design.md)
- [Postman API collection](Docs/collection/movie-ticketing-platform.postman_collection.json)

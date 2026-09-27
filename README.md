# Movie Ticketing Platform

A Java/Spring Boot backend for a movie-ticketing platform covering multiple cities, theatres, shows, and seat-level booking.

The repository is being developed one capability at a time as a monolith. Identity, JWT authentication, the public city catalogue, theatre administration, the movie catalogue, show scheduling, show discovery, show-specific seat inventory, temporary customer seat holds, confirmed bookings, customer booking history, whole-booking cancellation, and local booking lifecycle notifications are implemented. Payments, refunds, and external notification delivery remain future work.

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
- All-or-nothing customer holds for one or more show seats.
- PostgreSQL row locking that permits only one winner for overlapping concurrent hold requests.
- Configurable hold duration and query-driven lazy expiry without a cron job.
- Customer-owned hold detail and expiry-aware show-seat/show-search availability.
- Atomic conversion of an owned active hold into one confirmed booking.
- Retry-safe booking confirmation with one booking per hold.
- `BOOKED` show-seat state with database-enforced booking-item ownership.
- Customer-owned booking detail and paginated booking history.
- Atomic, owner-scoped whole-booking cancellation before show start.
- Idempotent duplicate cancellation with transactional release of every booked show seat.
- Pluggable confirmation and cancellation notifications through a channel-neutral sender port.
- Local structured-log delivery after transaction commit, with notification failures isolated from successful booking operations.
- UTC timestamp storage and an injected UTC `Clock` for application time.

Theatre, auditorium, and physical-seat APIs are intentionally create/list-only. Updates, deletion, deactivation, accessibility attributes, and physical-seat pricing are not implemented. Holds and bookings operate on show-specific seat inventory rather than the physical-seat layout.

## Customer seat holds

An authenticated customer can request a temporary hold on one or more `showSeatId` values for a single show. The operation is all-or-nothing: if any requested seat is not available, no requested seat is held.

The concurrency boundary is the requested set of PostgreSQL `show_seat` rows. A transaction locks the complete set in stable UUID order, evaluates every seat, and only then creates one hold and assigns every seat. Concurrent requests that overlap on a seat serialize at that row; after the first transaction commits, the losing request observes the active hold and is rejected in full. No JVM-local lock is used.

Hold duration is external configuration and all expiry calculations use the injected UTC `Clock`. Expiry is lazy and query-driven: a seat is effectively available when it is stored as `AVAILABLE`, or when it is stored as `HELD` and its current hold has expired. Public seat reads and aggregate show-search availability counts use this combined condition. A cron job or scheduled expiry worker is not required for correctness.

Expired holds remain readable as `EXPIRED`, while their seats can be acquired by a new hold. Historical hold items are retained and the current hold pointer on each show seat is replaced transactionally. See the [customer seat hold design](Docs/designs/customer-seat-hold-design.md) for the detailed transaction and schema.

## Booking confirmation

An authenticated customer can convert their own active hold into one `CONFIRMED` booking. Confirmation locks the owned hold and then its complete show-seat set. It creates the booking, creates and flushes every booking item, and only then changes the seats to `BOOKED`. The transaction clears each seat's hold pointer and assigns `current_booking_id`; a composite foreign key ensures that pointer identifies a booking item for the exact seat.

Confirmation is naturally retry-safe. `booking.source_hold_id` is unique, so the first request creates the booking with `201 Created` and a later retry returns the same booking with `200 OK`. A lost HTTP response therefore cannot create a duplicate booking. Booked seats are excluded from public availability and available-seat counts.

Booking detail and history are owner-scoped using the account ID from the verified JWT. History uses deterministic newest-first pagination and calculates seat counts in the page query rather than querying once per booking. A newly confirmed booking publishes one lifecycle event. An explicit `AFTER_COMMIT` transactional listener, with fallback execution disabled, sends the notification through the configured adapter. Idempotent confirmation replays publish no event and send no duplicate notification. See the [booking confirmation design](Docs/designs/booking-confirmation-design.md) for the confirmation transaction and schema invariants, and the [notification flow design](Docs/designs/notification-flow-design.md) for the adapter boundary and failure semantics.

## Booking cancellation

An authenticated customer can cancel their complete booking while the post-lock UTC cancellation instant is strictly before the show start. Partial cancellation is not supported. The operation locks the owner-scoped booking row and then all of its show seats in stable UUID order. It changes the booking to `CANCELLED`, records `cancelledAt`, releases every seat to `AVAILABLE`, and commits once. Any failure rolls back the complete operation.

Cancellation preserves the booking, items, original prices, confirmation timestamp, and source hold. The source hold remains `CONVERTED`, so a cancelled booking cannot be recreated from the same hold. Duplicate cancellation requests are idempotent and return the same cancelled booking with `200 OK`. Released seats immediately participate in public availability and can be held again. Only the first successful cancellation transition publishes an event; the same after-commit notification path handles it, while cancellation replays send nothing. Refunds, payment changes, and cancellation reasons remain out of scope. See the [booking cancellation design](Docs/designs/booking-cancellation-design.md) for the lock ordering and race analysis.

## Technology

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper 3.3.4 using Maven 3.9.11
- PostgreSQL
- Flyway
- Spring Data JPA
- Spring Security and OAuth2 Resource Server JWT support
- JUnit 5, AssertJ, MockMvc, and Testcontainers

## Key assumptions and design trade-offs

- The assignment's `admin` role is interpreted as a theatre administrator, not a platform-wide administrator. Signup is open, and ownership checks restrict each administrator to their own theatre hierarchy.
- Cities are a fixed, public catalogue of 50 Flyway-seeded Indian cities. City management APIs are intentionally omitted.
- Movies are global and immediately visible. The intentionally small duplicate key is normalized title, language code, and runtime.
- Physical seats are reusable layout definitions. Prices and sellable availability belong to immutable show-seat snapshots created when a show is scheduled.
- `REGULAR` and `PREMIUM` are layout tiers. Weekend pricing is not a tier or rule engine; an administrator submits final tier prices independently for every show.
- Expired holds release seats logically through expiry-aware queries. Correctness does not depend on a scheduler deleting or updating expired holds.
- Direct confirmation creates a booking without payment. Cancellation releases the complete booking before show start but performs no refund.
- Confirmation and cancellation notifications are after-commit and best effort. The local logging adapter proves the pluggable boundary but does not provide durable external delivery or reminders.
- PostgreSQL transactions, row locks, constraints, and stable lock ordering are the concurrency authority. No JVM-local lock participates in correctness.
- All timestamps are stored in UTC. `Asia/Kolkata` is used only to translate a customer's show-search date into UTC bounds.

See the [project HLD and LLD](Docs/designs/project-architecture-hld-lld.md) for the complete current-state architecture, transaction boundaries, package dependencies, schema invariants, and extension points.

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
|- hold/
|  |- api/             Customer hold HTTP contracts
|  |- application/     Atomic acquisition, expiry, and owner-scoped reads
|  |- config/          Required hold-duration configuration
|  `- domain/          Hold header, items, and repositories
|- booking/
|  |- api/             Confirmation, cancellation, owned detail, and history contracts
|  |- application/     Locking, confirmation, cancellation, retry, and query use cases
|  `- domain/          Booking header, items, state, and repositories
|- notification/
|  |- application/     After-commit listeners, messages, service, and sender port
|  `- adapter/logging/ Local structured-log sender
`- shared/api/         Common problem response handling

src/main/resources/
|- application.yml
`- db/migration/       Flyway migrations

Docs/
|- designs/            Feature designs and the project HLD/LLD
|- collection/         Import-ready Postman collection
|- AGENTS.md            Repository development instructions
|- Prompts.md           Chronological development prompt log
`- skills_used.md       Skills record required by the assignment

scripts/
`- api-demo.py          Self-checking end-to-end API demonstration

mvnw, mvnw.cmd          Pinned Maven Wrapper entry points
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
export BOOKING_HOLD_DURATION=PT5M
export NOTIFICATION_PROVIDER=logging
```

`JWT_SECRET` must be valid Base64 that decodes to at least 32 random bytes. It is required at startup and must not be committed.

`BOOKING_HOLD_DURATION` is a required positive ISO-8601 duration. `PT5M` configures five-minute holds; choose the operational value appropriate for the environment.

`NOTIFICATION_PROVIDER` defaults to `logging`. The current adapter writes structured confirmation and cancellation messages to the application log. An unsupported value intentionally prevents startup because no sender would be configured.

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

Unit tests run without external services. Identity, theatre-management, movie/show, seat-hold, and booking integration tests use PostgreSQL 16 through Testcontainers and require an active Docker-compatible runtime. When no compatible runtime is available, those tests are reported as skipped rather than using a different database engine.

Run the complete build lifecycle with:

```bash
./mvnw verify
```

## Running the API behavior demo

Run the application with a five-second hold duration so the expiry scenario completes quickly:

```bash
BOOKING_HOLD_DURATION=PT5S ./mvnw spring-boot:run
```

With the application and PostgreSQL running, execute the complete API demonstration with one command:

```bash
./scripts/api-demo.py
```

The script uses only Python 3.10+ standard-library modules. It creates uniquely named demo accounts and resources, checks every response, and exits non-zero when an invariant fails. Override the target when the application is not using the default address:

```bash
BASE_URL=http://localhost:8081 ./scripts/api-demo.py
```

The demonstration covers:

- public catalogue and show discovery access;
- customer and theatre-admin signup, shared sign-in, role restrictions, and owner-scoped theatre data;
- required pricing for every physical-seat tier;
- concurrent overlapping show creation with one winner per auditorium;
- concurrent overlapping holds with one winner and all-or-nothing seat acquisition;
- concurrent confirmation, serial confirmation retry, stable booking identity, and one history entry;
- customer ownership hiding for holds and bookings;
- concurrent and repeated cancellation with a stable cancellation timestamp;
- immediate seat availability and reacquisition after cancellation; and
- five-second hold expiry, lazy seat availability, seat reacquisition, and rejected confirmation of the expired hold.

The current API has no deletion endpoints, so successful demo records remain in the configured database. Every run uses unique names and emails and can safely coexist with earlier runs.

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

Malformed JSON returns `400 MALFORMED_REQUEST`. Bean-validation failures, missing required query parameters, and incorrectly typed query parameters return `400 VALIDATION_FAILED` with field-level entries in `violations`. These errors use the same problem response envelope across all APIs.

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

## Customer seat hold API

Both endpoints require `Authorization: Bearer <access-token>` for an account with role `CUSTOMER`. Theatre administrators cannot create or read customer holds.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/shows/{showId}/holds` | Atomically hold one or more seats for the configured duration. |
| `GET` | `/api/v1/holds/{holdId}` | Read a hold owned by the authenticated customer. |

Create a hold:

```json
{
  "showSeatIds": [
    "c4d7c07e-56eb-4abf-8aba-3b4a7b631375",
    "d9c35647-f47f-43a5-8321-d4cc55459134"
  ]
}
```

Successful response — `201 Created`:

```json
{
  "id": "973033f0-57bd-4ec3-bf42-d9a24bea39bb",
  "showId": "6e6e5731-8c4b-4bcb-a0cf-f36a45233b6c",
  "createdAt": "2026-09-25T12:00:00Z",
  "expiresAt": "2026-09-25T12:05:00Z",
  "status": "ACTIVE",
  "seats": [
    {
      "showSeatId": "c4d7c07e-56eb-4abf-8aba-3b4a7b631375",
      "rowLabel": "A",
      "seatNumber": 1,
      "seatLabel": "A1",
      "tier": "REGULAR",
      "price": {
        "amount": 250.00,
        "currency": "INR"
      }
    }
  ]
}
```

`status` is derived as `ACTIVE` while the current UTC instant is before `expiresAt`, `EXPIRED` after that boundary, or `CONVERTED` once the hold has produced a booking. Expiry does not require a database update. The hold duration starts after all requested seat locks are acquired, so lock waiting does not consume the customer's hold window.

The seat-ID list must be non-empty, contain no nulls, contain no duplicates, and refer entirely to the show in the URL. If any requested seat is unknown, belongs to another show, or has an active hold, the request returns `409 SEATS_UNAVAILABLE` and changes nothing. A show that has started returns `409 SHOW_ALREADY_STARTED`. Unknown or cross-customer hold reads return `404 HOLD_NOT_FOUND`.

The first implementation intentionally has no hard seat-count cap, active-hold-per-customer limit, idempotency key, early-release endpoint, or custom database lock timeout. These policies can be added in a later design iteration.

## Customer booking API

All booking endpoints require `Authorization: Bearer <access-token>` for an account with role `CUSTOMER`.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/holds/{holdId}/booking` | Atomically confirm the authenticated customer's active hold. |
| `GET` | `/api/v1/bookings/{bookingId}` | Read a confirmed or cancelled booking owned by the authenticated customer. |
| `GET` | `/api/v1/bookings?page=0&size=20` | List only the authenticated customer's bookings, newest first. |
| `POST` | `/api/v1/bookings/{bookingId}/cancellation` | Atomically cancel the authenticated customer's complete booking before show start. |

Confirmation takes no request body. A successful response contains the booking ID, source hold, show, confirmation time, total price, and complete seat set. The first confirmation returns `201`; an exact retry returns the same representation with `200`.

History defaults to page `0` and size `20`, with a maximum size of `100`. Its compact entries contain the booking ID, show ID, status, confirmation time, nullable cancellation time, total price, and seat count. Use the detail endpoint for individual seat information. Booking detail also includes nullable `cancelledAt`.

Cancellation takes no request body and always covers the complete booking. Its first success and exact retries return `200`. Important booking error codes are `BOOKING_NOT_FOUND`, `BOOKING_NO_LONGER_OWNS_SEATS`, `HOLD_EXPIRED`, `HOLD_NO_LONGER_OWNS_SEATS`, and `SHOW_ALREADY_STARTED`. Missing and cross-customer resources use the same not-found response.

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
- Dynamic show-pricing rules and discount codes.
- Payments.
- Partial booking cancellation and refunds.
- Durable notification delivery, retries, external channels, customer notification history, and reminder notifications.

The assignment continues to exclude a frontend, deployment/containerization, CI/CD, microservices, advanced authentication, and production-grade monitoring.

## Documentation

- [Project HLD and LLD](Docs/designs/project-architecture-hld-lld.md)
- [Agent instructions](Docs/AGENTS.md)
- [Development prompts](Docs/Prompts.md)
- [Skills used](Docs/skills_used.md)
- [Identity and authentication design](Docs/designs/identity-authentication-design.md)
- [Theatre administration design](Docs/designs/theatre-admin-management-design.md)
- [Movie catalogue and show scheduling design](Docs/designs/movie-catalogue-and-show-scheduling-design.md)
- [Customer seat hold design](Docs/designs/customer-seat-hold-design.md)
- [Booking confirmation design](Docs/designs/booking-confirmation-design.md)
- [Booking cancellation design](Docs/designs/booking-cancellation-design.md)
- [Booking lifecycle notification design](Docs/designs/notification-flow-design.md)
- [Postman API collection](Docs/collection/movie-ticketing-platform.postman_collection.json)

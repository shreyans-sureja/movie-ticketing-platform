# Agent Instructions

Follow these instructions for all work in this repository unless the user explicitly changes them.

## Confirmed technical constraints

- Use Java 21.
- Use Spring Boot.
- Use Maven.
- Use PostgreSQL.
- Use Flyway for database migrations.
- Keep the application as a monolith.
- Prefer standard Java and Spring Boot capabilities. Introduce another dependency only when its need has been discussed and agreed.

## Design process

- Do not infer or lock in domain models, modules, packages, API contracts, database schemas, authentication mechanisms, concurrency strategies, or infrastructure patterns before they are discussed.
- Work through design decisions with the user one step at a time.
- Clearly separate confirmed decisions from open questions.
- Record an assumption only after the user accepts it.
- Do not implement application code until the user explicitly asks for implementation.
- Keep the README aligned with confirmed decisions only.

## Development record

- Append every user development prompt to `Docs/Prompts.md` in chronological order.
- Preserve prompt wording. Do not add assistant responses, summaries, reasoning, or implementation notes to the prompt log.
- Preserve unrelated user changes in the repository.
- Keep changes small and focused on the current requested step.

## Quality baseline

- Use clear names and straightforward code.
- Keep secrets and local credentials out of version control.
- Add appropriate automated tests when implementation begins.
- Run the relevant Maven tests before declaring an implementation task complete.
- Keep documentation consistent with the behavior that actually exists.

## Assignment boundaries

Unless the user changes the scope, do not add:

- A UI or frontend.
- Deployment or containerization configuration.
- CI/CD configuration.
- Microservices or distributed-system infrastructure.
- Advanced authentication such as OAuth, SSO, or MFA.
- Production-grade observability, monitoring, or alerting.


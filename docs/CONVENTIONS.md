# Conventions

## Packages

```
com.b0ss.crm
├── auth        login, token issuance
├── user        user entity and lookup
├── customer    customer aggregate
├── followup    follow-up notes on a customer
├── report      dashboard counters
├── common      shared error handling and paging
└── config      security, JWT, OpenAPI
```

Each feature package follows `Entity / Repository / Service / Controller / dto`.

## API rules

- Base path `/api`, JSON only.
- Errors always use the `ApiError` shape:
  `{ timestamp, status, code, message, path, fieldErrors? }`
- Codes: `VALIDATION_FAILED` 400, `BAD_REQUEST` 400, `UNAUTHORIZED` 401,
  `BAD_CREDENTIALS` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `CONFLICT` 409,
  `INTERNAL_ERROR` 500.
- Paged responses always use `PageResponse`:
  `{ content, page, size, totalElements, totalPages }`.
- Sort parameter is `field,direction`; only whitelisted fields are accepted,
  anything else silently falls back to `createdAt,desc`.
- Page size is clamped to 1..100, page to >= 0.

## Database

- Schema is owned by Flyway (`src/main/resources/db/migration`).
  Hibernate runs with `ddl-auto: validate` and must never create tables.
- Migration names: `V<n>__<snake_case_description>.sql`.
- Timestamps are `TIMESTAMPTZ`; entities use `java.time.Instant`.
- Deleting a customer cascades to its follow-ups.

## Security

- Stateless JWT (HS256). Secret from `JWT_SECRET`, minimum 32 bytes.
- Claims: `sub` (username), `role` (ADMIN | SALES), `display_name`.
- Authorisation is checked at the method level with `@PreAuthorize`.
- Passwords are BCrypt; the raw password never leaves the login endpoint.

## Code style

- Constructor injection only, no field injection.
- DTOs are Java `record`s; entities are classes with explicit accessors.
- No Lombok.
- Service methods that read are `@Transactional(readOnly = true)`.
- Controllers contain no business logic.

## Git

Commit messages follow Conventional Commits, one logical change per commit.
`target/`, `node_modules/`, `.env` and local data are never committed.

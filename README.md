# Blog API

A Spring Boot REST API for a blog, with user registration, JWT authentication, post and comment management, and
separate user/admin roles. PostgreSQL stores application data; Flyway manages the production schema.

## Requirements

- Java 21 or newer
- PostgreSQL
- PowerShell on Windows (commands below use the Maven wrapper)

## Local setup

1. Create a PostgreSQL database named `blog` and note its username and password.
2. Edit the root `.env` file with your local values. Spring Boot imports this file when the app starts from the
   project root. The admin bootstrap reads its two credentials directly from `.env`, so stale PowerShell variables
   cannot override the local admin password. The file is ignored by Git; never commit it.

   ```dotenv
   DATABASE_URL=jdbc:postgresql://localhost:5432/blog
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=replace-with-your-local-postgres-password
   BLOG_JWT_SECRET=replace-with-a-random-secret-at-least-32-bytes
   BLOG_ADMIN_EMAIL=admin@example.com
   BLOG_ADMIN_PASSWORD=replace-with-a-strong-password
   ```

   Generate a JWT secret in PowerShell with:

   ```powershell
   [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   ```

   `BLOG_ADMIN_EMAIL` and `BLOG_ADMIN_PASSWORD` are optional; if either is missing, the app does not bootstrap an
   administrator. The admin password must be 12-72 characters. Public registration always creates a `USER`.

3. Start the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Flyway applies the database migrations at startup.

## API and authentication

- Swagger UI: [http://localhost:8080/swagger/swagger/index.html](http://localhost:8080/swagger/swagger/index.html)
- OpenAPI document: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Register: `POST /api/auth/register`
- Log in: `POST /api/auth/login`

Registration is public. Login returns a JWT access token that expires after 15 minutes. Paste the token into
Swagger's **Authorize** dialog; Swagger sends it as a bearer token for protected operations. The auth endpoints,
Swagger UI, OpenAPI document, and GET routes for published posts and their comments are public. Other API endpoints
require a valid token. Draft posts are only returned in the authenticated author's `/api/me/posts` list or admin
post listing; the public slug and comment routes only resolve published posts.

API errors use a consistent JSON response with a timestamp, HTTP status, error name, message, request path, and
`fieldErrors` array. Validation failures include the affected field and validation message; unexpected server
errors are logged and return a generic message without exposing internal details.

### Main endpoints

| Method | Endpoint | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public; creates a `USER` |
| `POST` | `/api/auth/login` | Public; returns a JWT |
| `GET` | `/api/posts` | Public; lists published posts |
| `GET` | `/api/posts/{slug}` | Public; reads a published post |
| `GET` | `/api/posts/{slug}/comments` | Public; lists comments |
| `POST` | `/api/posts` | Authenticated; creates a post |
| `GET` | `/api/me/posts` | Authenticated; lists your posts |
| `PUT` / `DELETE` | `/api/me/posts/{id}` | Post author or admin |
| `POST` | `/api/posts/{slug}/comments` | Authenticated |
| `DELETE` | `/api/comments/{id}` | Comment author or admin |
| `GET` | `/api/admin/users` | Admin |
| `GET` | `/api/admin/posts` | Admin |
| `PATCH` | `/api/admin/posts/{id}/status` | Admin; changes post status |

List endpoints expose separate query parameters in Swagger. Post and user lists support `page`, `size`, `sort`, and
`direction`; comments support `page`, `size`, and `direction`. Page numbering starts at zero, the default size is
20, and the maximum size is 100. Post sort values are `CREATED_AT`, `UPDATED_AT`, and `TITLE`. User sort values are
`CREATED_AT`, `DISPLAY_NAME`, and `EMAIL`. Post lists support a `status` filter (`DRAFT` or `PUBLISHED`); admin user
lists support a `role` filter (`USER` or `ADMIN`).

## Tests and build

Tests use an in-memory H2 database and do not require Docker or PostgreSQL:

```powershell
.\mvnw.cmd test
```

Build the application:

```powershell
.\mvnw.cmd package
```

The H2 test profile validates application startup and entity wiring, but skips Flyway and does not validate the
PostgreSQL migration.

## Metrics

OTLP metrics export is disabled by default, so local development does not need an OTLP collector. To enable it in an
environment with a collector, set `OTLP_METRICS_ENABLED=true` and `MANAGEMENT_OTLP_METRICS_EXPORT_URL` to the
collector's metrics endpoint. Prometheus metrics are also available through the configured registry.

## Production secrets

Do not store production credentials in `application.properties` or commit `.env`. Supply `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, `BLOG_JWT_SECRET`, and optional admin bootstrap values through your
deployment platform's secret manager or protected environment configuration. Use a private, randomly generated JWT
secret and rotate any secret that may have been exposed.

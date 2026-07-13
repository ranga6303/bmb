# Configuration

## Main Profile: `src/main/resources/application.properties`

| Property | Source/default | Meaning |
|---|---|---|
| `server.port` | `${PORT:8080}` | HTTP port. |
| `app.jwt.secret` | `${JWT_SECRET}` | Base64 HMAC secret for JWT signing. Required. |
| `app.jwt.expiration-ms` | `${JWT_EXPIRATION_MS:86400000}` | Access token lifetime in milliseconds. |
| `app.base-url` | `${APP_BASE_URL:http://localhost:8080}` | Base URL injected into `AuthService`; currently not used in URL generation. |
| `spring.datasource.url` | `${DB_URL}` | JDBC URL. Required. |
| `spring.datasource.driverClassName` | `${DB_DRIVER_CLASS_NAME:com.mysql.cj.jdbc.Driver}` | JDBC driver class. |
| `spring.datasource.username` | `${DB_USERNAME}` | DB username. Required. |
| `spring.datasource.password` | `${DB_PASSWORD}` | DB password. Required. |
| `spring.jpa.hibernate.ddl-auto` | `${JPA_DDL_AUTO:update}` | Hibernate schema mode. |
| `spring.jpa.show-sql` | `false` | SQL logging disabled. |
| `spring.jpa.open-in-view` | `false` | Lazy loading outside transactions disabled. |
| `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.MySQLDialect` | Hibernate dialect. |
| `resend.api.key` | literal value in file | Resend API key used by `EmailConfiguration`. This should be moved to `${RESEND_API_KEY}`. |
| `app.mail.from` | `${MAIL_FROM:onboarding@resend.dev}` | Sender address for OTP emails. |
| `app.admin.username` | `${ADMIN_USERNAME}` | Bootstrap admin username. |
| `app.admin.email` | `${ADMIN_EMAIL}` | Bootstrap admin email. |
| `app.admin.default-password` | `${ADMIN_PASSWORD}` | Bootstrap admin password. |

## Test Profile: `src/test/resources/application-test.properties`

| Property | Value / meaning |
|---|---|
| `spring.datasource.url` | In-memory H2 database with MySQL mode. |
| `spring.datasource.driverClassName` | `org.h2.Driver`. |
| `spring.datasource.username` / `password` | `sa` / empty. |
| `spring.jpa.hibernate.ddl-auto` | `create-drop`. |
| `spring.mail.host` / `port` | localhost:2525. |
| `app.jwt.secret` | test-only Base64 secret. |
| `app.jwt.expiration-ms` | 3600000. |

## Environment Variables Needed To Run

| Variable | Required | Notes |
|---|---|---|
| `PORT` | no | Defaults to 8080. |
| `JWT_SECRET` | yes | Must be Base64 and long enough for HS256 key creation. |
| `JWT_EXPIRATION_MS` | no | Defaults to 24 hours. |
| `APP_BASE_URL` | no | Defaults to localhost. |
| `DB_URL` | yes | MySQL JDBC URL in normal runtime. |
| `DB_DRIVER_CLASS_NAME` | no | Defaults to MySQL driver. |
| `DB_USERNAME` | yes | Database user. |
| `DB_PASSWORD` | yes | Database password. |
| `JPA_DDL_AUTO` | no | Defaults to `update`; consider `validate` in production with migrations. |
| `RESEND_API_KEY` | intended | `.env` contains it, but the main properties file does not currently reference it. |
| `MAIL_FROM` | no | Defaults to Resend onboarding address. |
| `ADMIN_USERNAME` | yes for predictable bootstrap | Used by `AdminBootstrap`. |
| `ADMIN_EMAIL` | yes for predictable bootstrap | Used by `AdminBootstrap`. |
| `ADMIN_PASSWORD` | yes for predictable bootstrap | Used by `AdminBootstrap`. |

## Email Bean Selection

`EmailConfiguration` creates `SmtpEmailService` when property `resend.api.key` exists. Otherwise it creates `NoOpEmailService`, whose send methods throw `CustomException`.

Because `application.properties` currently defines `resend.api.key` directly, `SmtpEmailService` is always enabled in the main profile.

## Local `.env`

`.env` is loaded by `DemoApplication` and ignored by Git. It contains local runtime values and secrets. Do not commit it or copy literal secret values into docs, logs, tickets, or screenshots.

## Deployment

`Procfile` starts the already-built artifact:

```text
web: java -jar target/demo-0.0.1-SNAPSHOT.jar
```


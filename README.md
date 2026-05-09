# CityFlow

> A modular backend for a city review & seckill platform. Built to demonstrate
> Redis-backed distributed locking, JPA/Flyway-managed schema, JWT auth, and
> Docker-based local deployment.

## Why this project

CityFlow is a cloud-ready backend for a city review & seckill platform —
users browse shops, post reviews, and compete for limited-edition flash-sale
vouchers. The codebase exercises the patterns a typical high-traffic
e-commerce backend needs:

- **Spring Boot + JPA + Flyway** for a versioned, reproducible data layer,
  evolved through stage→merge migrations
- **Spring Security + JWT** for stateless authentication
- **Redis + Redisson** for cache-aside reads and distributed locks on the
  seckill hot path — keeping order placement consistent under concurrent load
- **Kafka** decouples order placement from persistence: the HTTP path stays
  on Redis-only checks while a consumer drains writes to MySQL asynchronously
- **Structured error handling + traceId** — every request gets an 8-char
    trace ID echoed in `X-Trace-Id`; failures return stable error codes like
    `VOUCHER_NOT_FOUND` instead of free-text strings, so logs and clients
    can branch reliably
- **Docker Compose** for a one-command local stack;
  **GitHub Actions** ships the image to GHCR on every push to `main`

## Tech stack

| Layer | Choice | Version | Why |
|---|---|---|---|
| Language | Java    | 17 |
| Framework | Spring Boot | 2.7.18 | Batteries-included |
| ORM | Spring Data JPA + Hibernate | 5.6 | Repository pattern, declarative tx |
| Mapping | MapStruct | 1.5 | Compile-time DTO ↔ entity conversion |
| DB | MySQL | 8.0 | Relational baseline |
| Migration | Flyway | 10.17 | Versioned, reproducible schema |
| Cache & lock | Redis + Redisson | 7 / 3.23 | Cache-aside + distributed RLock |
| Messaging | Kafka | 3.x | Async order pipeline (decoupling write path) |
| Auth | Spring Security + jjwt | 5.7 / 0.11.5 | Stateless JWT in `Authorization` header |
| Reverse proxy | Nginx | 1.25 | Upstream routing + static assets |
| Build | Maven | 3.x | Standard JVM build |
| Container | Docker + Compose | v2 | One-command local stack |
| CI | GitHub Actions | — | Build + push image to GHCR |
| Tests | JUnit 5 + Mockito | — | Unit & integration coverage |
| Logging | SLF4J + Logback + MDC | — | Per-request traceId, structured exception logging |

## Quick start (5 minutes)



## API examples

### Request a voucher that doesn't exist

```bash
$ curl -i -X POST http://localhost:8080/voucher-order/seckill/99999 \
       -H "authorization: $TOKEN"

HTTP/1.1 200
X-Trace-Id: efab1825
Content-Type: application/json

{
  "success": false,
  "code": "VOUCHER_NOT_FOUND",
  "errorMsg": "voucherId=99999"
}
```

The `X-Trace-Id` header matches the `[efab1825]` tag in the application log:

```
WARN [efab1825] c.c.config.WebExceptionAdvice : Business exception: code=VOUCHER_NOT_FOUND msg=voucherId=99999
```

Customer-support hands the trace ID to ops; ops `grep` the log and
retrieves the full request chain — JWT auth, service calls, Redis ops —
under a single ID.



## Project structure



## Troubleshooting






# CityFlow — Design Decisions

This document records the non-obvious architectural choices in CityFlow,
the reasoning behind them, and the trade-offs accepted. It serves as
both ADR (architecture decision record) and an interview reference.

---

## ADR-001: Monolith, not microservices

### Context

CityFlow models a city review platform with concurrent flash-sale traffic.
The "obvious" senior-engineer move would be to split it into services —
user-service, shop-service, order-service, etc. — with separate databases
and inter-service communication.

### Decision

Keep CityFlow as a single deployable Spring Boot application. Use packages
(`web`, `service`, `repository`, `kafka`, ...) for logical separation,
but not network boundaries.

### Reasoning

A real microservices split costs:

- **Distributed transactions**: an order that touches `tb_user`,
  `tb_voucher`, and `tb_voucher_order` becomes a saga or 2PC across
  services
- **Operational overhead**: 3+ service repos, 3+ pipelines, service
  discovery, request tracing across processes, more failure modes
- **Local dev friction**: `docker compose up -d` now has to wait for 4–5
  apps to be healthy instead of 1
- **Testing friction**: integration tests need to coordinate multiple
  services or mock service boundaries

The benefits of microservices (independent deploy, independent scale,
team isolation) accrue at organizational scale — multiple teams owning
distinct domains. A solo project with one developer does not realize them.

The architecture I chose preserves the *option* to split later: the
business logic is already grouped by domain (`service/ShopService`,
`service/VoucherOrderService`, `service/UserService`), and the Kafka
layer is the natural fault line where a split would happen first —
the order consumer is already a logical unit.

### Trade-off accepted

A single failing component (e.g. the Kafka consumer thread) lives in
the same JVM as the HTTP path. Mitigated by separate thread pools, but
not by hard process isolation.

### When I'd revisit

When traffic patterns diverge enough that one component needs to scale
independently — for example, if review-writing stays at 10 RPS but
flash-sale spikes to 10K RPS during launches, splitting out the
seckill consumer becomes worthwhile.

---

## Why Kafka for the order pipeline

### Context

The original seckill path was synchronous: acquire Redisson lock → read
stock from DB → DECR stock → INSERT order → release lock → return.
Under load, the DB write was the bottleneck (lock + 2 DB round-trips
per request).

### Decision

Split into two stages:

1. **Hot path (HTTP thread)** — atomic Redis ops (`DECR` stock + `SADD`
   user) + publish `OrderCreatedEvent` to Kafka + return order ID.
   No DB I/O. No distributed lock.

2. **Cold path (consumer thread)** — drain events from Kafka, check
   `existsById` for idempotency, persist to MySQL.

### Reasoning

- **Atomicity**: Redis `DECR` is a single-key atomic operation. No
  separate lock is needed for stock decrement correctness. Same for
  `SADD` (duplicate-purchase check).
- **Latency**: HTTP p50 went from ~80 ms (lock + 2 DB queries) to
  ~27 ms (1 Redis RTT + 1 Kafka send). The 3× speedup is reproducible
  and visible in the API examples in the README.
- **Backpressure smoothing**: a burst of 1000 requests within 100 ms
  enters Kafka as 1000 events but drains to MySQL at the consumer's
  comfortable rate. The DB never sees spike load.
- **Durability**: Kafka acts as a write-ahead log. If the app crashes
  between Redis decrement and DB persistence, the event is still in
  Kafka. On restart, the consumer replays from its committed offset.

### Trade-off accepted

- **Eventual consistency** between Redis stock counter and MySQL stock
  counter. The window is bounded (typical drain latency ~500 ms) and
  the `existsById` idempotency check makes consumer retries safe.
- **No exactly-once delivery**. Kafka gives at-least-once; the
  idempotent consumer turns it into "effective exactly-once at the
  application layer." True exactly-once via Kafka transactions would
  add coordination overhead and complicate the DB write path —
  unjustified for this workload.

### What I'd add for production

- A consumer for the dead-letter topic (today it's a passive trap)
- Partitioning by `voucherId` to enable horizontal scaling
- Rate limiting at the HTTP edge (Nginx or Spring Cloud Gateway) to
  bound Kafka publish rate during spikes

---

## Why I didn't move the Redis ops to a Lua script

A Lua script would atomicize the `DECR` + `SADD` pair (today they are
two separate round-trips, so a crash between them leaves a slight
inconsistency — stock decremented but user not recorded in the dedup
set).

I considered it and chose not to:

- The inconsistency window is **microseconds** between two pipelined
  Redis commands on the same connection
- Kafka has the event durably, and the consumer's `existsById` check
  handles replays correctly, so the system self-heals on restart
- The added complexity of versioning, testing, and debugging Lua
  scripts (no IDE support, no unit-test framework, opaque error
  messages) didn't justify closing such a narrow window

If this were production handling real money or audited compliance
events, I'd add it. For the current scale and reliability target the
trade-off is the other way.

---

## Why Flyway, not Hibernate `ddl-auto`

### Context

Spring Boot + JPA makes it tempting to set `ddl-auto: update` and let
Hibernate manage the schema. This is fine for prototypes; it's a
liability everywhere else.

### Decision

All schema changes go through versioned Flyway migrations in
`src/main/resources/db/migration/`. `ddl-auto: validate` runs on startup
to fail-fast if the schema and entity definitions disagree.

### Reasoning

- **Reproducibility**: a fresh database from `docker compose up` lands
  at the same schema state every time, walked through the same migration
  steps. CI gets exactly what production gets.
- **Reviewable**: schema changes are diff-able in PRs. A migration named
  `V6__add_index_user_email.sql` is a clear unit of review; a Hibernate
  annotation change buried in an entity isn't.
- **Production-safe**: rolling deploys can run migrations in a deploy
  step, separate from app startup. Hibernate `ddl-auto: update` cannot
  be safely run against a live production database.

---

## Stage→merge pattern for seed-data imports

CityFlow ships with real shops, vouchers, users, and blogs imported
from TSV exports. Loading these directly into the canonical tables
creates two problems:

1. **Type fragility**: the TSV has empty strings, oddly-formatted
   timestamps, and missing values. A direct `INSERT` blows up on
   `NOT NULL` violations and bad coercions.
2. **FK topology**: `tb_voucher` references `tb_shop`. `tb_blog`
   references both `tb_user` and `tb_shop`. The TSVs don't necessarily
   arrive in dependency order.

### Decision: three-phase migration

The schema bootstrap is split across three migrations:

| Version | File | Job |
|---|---|---|
| **V3** | `V3__stage_load.sql` | Create permissive `stage_*` buffer tables (all `VARCHAR`, all `NULL`-able); `LOAD DATA LOCAL INFILE` raw TSV bytes; `TRUNCATE` first so the migration is idempotent on re-run |
| **V4** | `V4__merge_core.sql` | Merge stage rows into the FK-free core tables `tb_user`, `tb_shop` via `INSERT … ON DUPLICATE KEY UPDATE`; `COALESCE` to fill `NOT NULL` defaults; reset `AUTO_INCREMENT` |
| **V5** | `V5__merge_refs.sql` | Merge dependent tables `tb_voucher` and `tb_blog`. Must run after V4 — relies on `tb_user.id` and `tb_shop.id` being present |

### Why this works

- **V3 "get the bytes in"** is decoupled from "clean and land them."
  If the TSV has bad bytes, only V3 fails, and only the stage tables
  are affected — `tb_shop` proper is untouched.
- **V4 / V5 split by FK topology**. Merging core tables before dependent
  ones guarantees that the dependent merges' FK lookups succeed.
- **Idempotent**. `ON DUPLICATE KEY UPDATE` against natural keys
  (`phone` for users; `(name, address)` for shops; `(shop_id, title)`
  for vouchers) means re-running the merge against an already-loaded
  database is a no-op.
- **Auditable**. Each TSV column passes through an explicit projection
  with `NULLIF` and `COALESCE` rules visible in code — no "magic"
  coercions hidden in an ETL tool.

### Trade-off accepted

This is more SQL than a single `INSERT`. The win is that production
operators can re-run the migration after fixing a bad TSV without
having to manually scrub partial data — a property that matters more
the bigger the dataset.

---

## Why idempotent consumer + DLT (not transactional exactly-once)

The `OrderCreatedConsumer`:

1. Reads an event from Kafka
2. Checks `voucherOrderRepository.existsById(orderId)`
3. If false → decrement DB stock + INSERT order
4. If true → log and skip (idempotent replay)

On exception, `DefaultErrorHandler` retries 3 times with 1 s back-off,
then `DeadLetterPublishingRecoverer` routes the failed record to
`seckill.order-created.DLT`. The main topic offset commits normally,
so one bad event never blocks the queue.

### Why this and not Kafka transactional API?

Kafka transactions give true exactly-once between Kafka topics, but my
consumer writes to MySQL — a different system. To make the Kafka commit
and the MySQL write transactional together, I'd need a transactional
outbox pattern, or 2PC via XA. Both add infrastructure cost that the
idempotent-insert pattern avoids for free.

The idempotency key is `orderId`, generated client-side at HTTP time via
snowflake. The HTTP path embeds it in the Kafka message before publishing,
so even on Kafka redelivery the consumer sees the same key and the
`existsById` check works.

---

## Known trade-offs (what I deliberately didn't build)

This is a portfolio project, not a production system. Things I'm aware
are missing:

| Not built | Why | Where I'd add it |
|---|---|---|
| **DLT consumer / replay tooling** | DLT exists as a passive trap; no auto-replay or alerting | A monitoring webhook + a `/admin/dlt/replay` endpoint |
| **Kafka partitioning** | Single partition; consumer is single-threaded | Partition by `voucherId`; multiple consumer instances; verify ordering still holds per key |
| **Rate limiting** | A determined client can saturate the Kafka topic | Nginx `limit_req` or Bucket4j at the controller level |
| **Distributed tracing** | TraceId is in-process MDC only, not propagated to Kafka headers | Spring Cloud Sleuth or OpenTelemetry with Kafka instrumentation |
| **JSON structured logging** | Plain logback pattern with trace IDs; not machine-parseable | `logstash-logback-encoder` once a log aggregator (Loki / ELK) is in scope |
| **Kubernetes manifests** | docker-compose only | `deployment.yaml` + `service.yaml` + HPA on Kafka consumer lag |
| **Real cloud deployment** | Local + GHCR only | EKS / GKE behind a real load balancer; managed RDS / ElastiCache / MSK |
| **Negative test for DLT routing** | Covered conceptually but no test asserts a poison message lands on the DLT | An `@EmbeddedKafka` test injecting a malformed event and verifying the DLT consumer count |

The omissions are choices, not oversights. Each has a clear path to
addition without restructuring the existing code.

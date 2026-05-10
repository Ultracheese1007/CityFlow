# CityFlow — 5-Minute Demo Walkthrough

This walkthrough takes you from a cold `docker compose up` to a
successful flash-sale order, an idempotent duplicate-purchase rejection,
and a peek at the async Kafka drain — in five `curl` commands.

> Pre-req: `docker compose up -d` is healthy. Verify with
> `curl -s http://localhost:8080/actuator/health` → `{"status":"UP"}`.

---

## Step 1 — Request a verification code

CityFlow's login flow is phone + SMS-code based (no password). The
`/user/code` endpoint accepts the phone as a query parameter,
generates a 6-digit code, and stashes it in the HTTP session.

```bash
curl -i -X POST 'http://localhost:8080/user/code?phone=13800138000'
```

```
HTTP/1.1 200
X-Trace-Id: a1b2c3d4
Content-Type: application/json

{"success":true}
```

The generated code is logged at DEBUG level on the server side. Grab
it from the logs:

```bash
docker compose logs cityflow-app | grep "发送短信验证码成功" | tail -1
# → 发送短信验证码成功，验证码：249813
```

---

## Step 2 — Log in with the code, capture the JWT

```bash
curl -s -X POST http://localhost:8080/user/login \
  -H 'Content-Type: application/json' \
  -c /tmp/cookie.jar \
  -d '{"phone":"13800138000","code":"249813"}'
```

```json
{"success":true,"data":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAxIn0..."}
```

The `data` field is the JWT — save it for subsequent calls:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/user/login \
  -H 'Content-Type: application/json' \
  -c /tmp/cookie.jar \
  -d '{"phone":"13800138000","code":"249813"}' | jq -r .data)
```

> Note: `/user/login` also sets a refresh cookie. Pass `-c /tmp/cookie.jar`
> to keep it for the duration of the demo — the JWT filter will use it
> to auto-refresh expired access tokens.

---

## Step 3 — Browse a shop (cache-aside)

```bash
curl -s http://localhost:8080/shop/1 -H "authorization: $TOKEN" | jq
```

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Bistro Tilburg",
    "typeId": 2,
    "openHours": "10:00-22:00"
  }
}
```

The first call reads MySQL and warms the Redis cache with TTL +
null-marker (defeats cache penetration). The next call returns from
Redis — same response, but the trace-ID's downstream log lines no
longer include the `SELECT` query.

---

## Step 4 — Place a flash-sale order (the headline feature)

```bash
time curl -i -X POST http://localhost:8080/voucher-order/seckill/3 \
  -H "authorization: $TOKEN"
```

```
HTTP/1.1 200
X-Trace-Id: 4aa2e5f4
Content-Type: application/json

{"success":true,"data":589965340662824961}

real    0m0.027s
```

The 27 ms is Redis-only — `DECR stock:3` + `SADD order:3 userId` +
publish `OrderCreatedEvent` to Kafka + return the snowflake order ID.
The HTTP thread never touches MySQL.

The async drain happens in the background. Confirm by tailing logs:

```bash
docker compose logs cityflow-app -f | grep OrderCreatedConsumer
```

```
20:06:51.162 INFO  OrderCreatedConsumer : Consuming order event: orderId=589965340662824961
20:06:51.451 INFO  OrderCreatedConsumer : Order 589965340662824961 persisted successfully
```

DB persistence completes ~550 ms after HTTP returns. Verify the row
landed:

```bash
docker exec cityflow-mysql mysql -uroot -proot cityflow \
  -e "SELECT id, user_id, voucher_id, status FROM tb_voucher_order ORDER BY id DESC LIMIT 1;"
```

---

## Step 5 — Try to buy again: structured error

```bash
curl -s -X POST http://localhost:8080/voucher-order/seckill/3 \
  -H "authorization: $TOKEN" | jq
```

```json
{
  "success": false,
  "code": "ALREADY_PURCHASED",
  "errorMsg": "用户已经购买过一次"
}
```

The duplicate-purchase check happens against the Redis `SADD` return
value — no DB query, no Kafka event published. Front-end clients branch
on the `code` field (`ALREADY_PURCHASED`, `STOCK_INSUFFICIENT`,
`VOUCHER_NOT_FOUND`, …) rather than parsing the human-readable message.

The `X-Trace-Id` on the response header lets ops `grep` the full request
chain by ID across JWT auth, service calls, and Redis operations — see
`TraceIdFilter` and the logback pattern in `application.yaml`.

---

## Bonus — Inspect Kafka topics directly

```bash
docker exec cityflow-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic seckill.order-created \
  --from-beginning --max-messages 5
```

```json
{"orderId":589965340662824961,"userId":1001,"voucherId":3,"timestamp":1715634410902}
```

And the dead-letter topic (empty in healthy operation):

```bash
docker exec cityflow-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic seckill.order-created.DLT \
  --from-beginning --timeout-ms 2000
```

---

## Other endpoints worth knowing

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/shop/of/type?typeId=2&current=1` | Paginated shops by category |
| `GET` | `/shop/of/name?name=bistro&current=1` | Search by name |
| `GET` | `/user/info/{id}` | Public user info |
| `GET` | `/actuator/health` | Health probe (no auth required) |
| `GET` | `/actuator/info` | App metadata (name, version, Java) |

---

## What to mention if walking someone through this

1. **27 ms HTTP** — point out it's Redis-only, no DB
2. **Order ID returned immediately** — but the row in MySQL doesn't
   exist for ~550 ms. Eventual consistency is a deliberate trade-off
3. **Structured `code` field** — not free-text errors. Phase 3 work.
4. **Trace IDs** — every response has `X-Trace-Id`, every log line
   carries the same ID via MDC. One `grep` retrieves the full chain.
5. **Idempotency** — the consumer's `existsById` check makes Kafka
   replays safe; explained in `docs/DESIGN.md`.

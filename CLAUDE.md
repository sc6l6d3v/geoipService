# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Scala REST service that resolves IP addresses to geolocation data using the [ipgeolocation.io](https://api.ipgeolocation.io) API. Results are cached in Redis and persisted to MongoDB.

## Build & Run

This is an SBT project (Scala 3.3.8, JDK 21, sbt 1.10.5).

```bash
sbt compile              # compile
sbt test                 # run all tests
sbt "testOnly *Spec"     # run a single test spec by name suffix
sbt assembly             # build fat JAR for Docker
sbt ~reStart             # hot-reload dev server (sbt-revolver), attaches debugger on port 5050
```

Environment variables are loaded from `.env` via sbt-dotenv. Required vars: `REDISKEY`, `REDISHOST`, `MONGOURI`, `GEOIPKEY`, `DBNAME`. Optional: `PORT` (default 8080), `BINDHOST` (default 0.0.0.0), `CLIENTPOOL`, `SERVERPOOL`, `COLLNAME` (default "ipdb").

Docker build: `./build-docker.sh` (reads `.env`, runs `sbt assembly` first).

## Architecture

**Request flow:** HTTP request → `GeoIPRoutes` → `GeoIP.getByIP` → check Redis cache → if miss, call ipgeolocation.io API → store in MongoDB → cache in Redis → return JSON response.

**Key layers:**

- `Main` — IOApp entry point. Wires up Redis, MongoDB, and STTP resources, then starts the server.
- `GeoIPServer` — Builds the http4s Ember server and composes the `HttpApp` with logging middleware.
- `routes/GeoIPRoutes` — Single route: `GET /ip/{address}`. Returns JSON geolocation data.
- `domains/GeoIP` — Core business logic. Implements the cache-check → API-fetch → MongoDB-insert → Redis-set pipeline.
- `domains/Cache` — Trait providing `getIPFromRedis` and `setRedisKey` operations.
- `domains/IP` / `IPinternal` — Case classes for IP geolocation data. `IP` decodes from the external API; `IPinternal` is used for encoding responses (both use zio-json).
- `config/RedisConfig` — Builds redis4cats `Resource` from `REDISHOST`/`REDISKEY` env vars.
- `config/MongodbConfig` — Parses MongoDB connection string into `MongoClientSettings` (uses `com.mongodb` Java driver classes directly).
- `util/DbClient` — Wraps mongo4cats `MongoClient` resource and collection access.
- `api/GeoIPApiUri` — Constructs the ipgeolocation.io API URL from the `GEOIPKEY` env var and requested IP.

## Test Framework

Tests use Specs2 5.x and Weaver (weaver-cats). The weaver test framework is registered in `build.sbt`. Existing tests cover `GeoIPApiUri` URL construction and `MongodbConfig` settings parsing.

## Key Libraries

- **http4s** (Ember server) — HTTP layer
- **cats-effect** (IO) — effect system
- **sttp** (client3 fs2) — HTTP client for external API calls
- **redis4cats** — Redis caching
- **mongo4cats** — MongoDB persistence (wraps the MongoDB Java driver)
- **zio-json** — JSON serialization/deserialization

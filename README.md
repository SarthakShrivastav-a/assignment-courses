# Course Platform API — Project Documentation

## Overview

A Spring Boot backend for a learning platform where users can browse courses, enroll, and track their progress. Built with Java 17, Spring Boot 4.0.2, PostgreSQL, Elasticsearch, and JWT authentication.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Database Design](#database-design)
4. [API Endpoints](#api-endpoints)
5. [Authentication Flow](#authentication-flow)
6. [Search Architecture](#search-architecture)
7. [Configuration & Profiles](#configuration--profiles)
8. [Docker Setup](#docker-setup)
9. [How to Run](#how-to-run)

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Language |
| Spring Boot 4.0.2 | Framework |
| Spring Data JPA / Hibernate | ORM and database access |
| PostgreSQL 16 | Primary database |
| Spring Security | Authentication framework |
| JJWT 0.12.6 | JWT token generation and validation |
| Elasticsearch 8.17.0 | Full-text search with fuzzy matching |
| Springdoc OpenAPI 2.8.4 | Swagger UI and API docs |
| Lombok | Boilerplate reduction |
| Docker / Docker Compose | Containerization |

---

## Project Structure

```
src/main/java/api/assignment/backend/
│
├── BackendApplication.java          # Entry point
│
├── entity/                          # JPA entities (database tables)
│   ├── Course.java                  # Course with string ID
│   ├── Topic.java                   # Belongs to a course
│   ├── Subtopic.java               # Belongs to a topic, has markdown content
│   ├── User.java                    # Auto-generated long ID, unique email
│   ├── Enrollment.java             # Links user to course (unique pair)
│   └── SubtopicProgress.java       # Links user to subtopic (unique pair)
│
├── repository/                      # Spring Data JPA repositories
│   ├── CourseRepository.java
│   ├── TopicRepository.java
│   ├── SubtopicRepository.java
│   ├── UserRepository.java         # findByEmail, existsByEmail
│   ├── EnrollmentRepository.java   # findByUserIdAndCourseId
│   └── SubtopicProgressRepository.java
│
├── dto/                             # Data Transfer Objects (request/response shapes)
│   ├── auth/
│   │   ├── RegisterRequest.java     # email + password (validated)
│   │   ├── RegisterResponse.java    # id + email + message
│   │   ├── LoginRequest.java        # email + password (validated)
│   │   └── LoginResponse.java       # token + email + expiresIn
│   ├── course/
│   │   ├── CourseListResponse.java  # wraps list of summaries
│   │   ├── CourseSummaryDto.java    # id, title, description, counts
│   │   ├── CourseDetailResponse.java# full nested course structure
│   │   ├── TopicDto.java
│   │   └── SubtopicDto.java
│   ├── enrollment/
│   │   ├── EnrollmentResponse.java  # enrollmentId, courseId, courseTitle, enrolledAt
│   │   └── ProgressResponse.java    # totals, percentage, topic progress, completed items
│   ├── progress/
│   │   ├── SubtopicCompleteResponse.java
│   │   ├── CompletedItemDto.java
│   │   └── TopicProgressDto.java    # per-topic completion tracking
│   └── search/
│       ├── SearchResponse.java      # query + results
│       ├── CourseSearchResult.java   # courseId + courseTitle + matches
│       └── SearchMatch.java         # type, topicTitle, subtopicId, snippet
│
├── service/                         # Business logic
│   ├── AuthService.java             # Register (BCrypt) + Login (JWT)
│   ├── CourseService.java           # List all, get by ID
│   ├── EnrollmentService.java       # Enroll user in course
│   ├── ProgressService.java         # Mark complete, get progress
│   └── SearchService.java          # ES search with PG fallback
│
├── controller/                      # REST endpoints
│   ├── AuthController.java          # /api/auth/*
│   ├── CourseController.java        # /api/courses/*
│   ├── EnrollmentController.java    # /api/courses/{id}/enroll
│   ├── ProgressController.java      # /api/subtopics/{id}/complete, /api/enrollments/{id}/progress
│   └── SearchController.java        # /api/search?q=...
│
├── security/                        # JWT authentication layer
│   ├── JwtTokenProvider.java        # Generate, validate, parse tokens
│   ├── JwtAuthenticationFilter.java # Intercepts requests, sets SecurityContext
│   └── CustomUserDetailsService.java# Loads user from DB for Spring Security
│
├── config/                          # Configuration beans
│   ├── SecurityConfig.java          # HTTP security rules, BCrypt, stateless sessions
│   ├── SwaggerConfig.java           # OpenAPI info + JWT security scheme
│   └── ElasticsearchConfig.java     # ES client bean (supports API key for Elastic Cloud)
│
├── exception/                       # Error handling
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice — maps exceptions to HTTP responses
│   ├── ErrorResponse.java           # { error, message, timestamp }
│   ├── ResourceNotFoundException.java   # 404
│   ├── DuplicateResourceException.java  # 409
│   └── NotEnrolledException.java        # 403
│
└── seed/
    └── DataLoader.java              # Loads courses.json into DB on startup if empty

src/main/resources/
├── application.yaml                 # Shared config
├── application-dev.yaml             # Local development overrides
├── application-prod.yaml            # Production overrides (env vars)
└── seed_data/courses.json           # 3 courses, 9 topics, 27 subtopics
```

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌──────────────┐
│   Course    │1     *│    Topic    │1     *│   Subtopic   │
│─────────────│───────│─────────────│───────│──────────────│
│ id (String) │       │ id (String) │       │ id (String)  │
│ title       │       │ title       │       │ title        │
│ description │       │ orderIndex  │       │ content(TEXT) │
│             │       │ course_id   │       │ orderIndex   │
│             │       │             │       │ topic_id     │
└─────────────┘       └─────────────┘       └──────────────┘

┌─────────────┐       ┌──────────────┐      ┌──────────────────┐
│    User     │       │  Enrollment  │      │ SubtopicProgress │
│─────────────│       │──────────────│      │──────────────────│
│ id (Long)   │       │ id (Long)    │      │ id (Long)        │
│ name        │       │ user_id      │      │ user_id          │
│ email (uniq)│       │ course_id    │      │ subtopic_id      │
│ password    │       │ enrolledAt   │      │ completedAt      │
│             │       │ UNIQUE(u,c)  │      │ UNIQUE(u,s)      │
└─────────────┘       └──────────────┘      └──────────────────┘
```

### Key Design Decisions

- **Course, Topic, Subtopic** use human-readable string IDs (e.g., "physics-101", "velocity") — set from seed data, not auto-generated.
- **User** uses auto-generated Long ID — standard for user tables.
- **Enrollment** has a unique constraint on (user_id, course_id) — prevents double enrollment.
- **SubtopicProgress** has a unique constraint on (user_id, subtopic_id) — ensures idempotent completion.
- **Subtopic.content** uses `TEXT` column type for long markdown content.
- All relationships use `CascadeType.ALL` from parent to child — saving a Course saves its Topics and Subtopics.

---

## API Endpoints

### Public (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/courses` | List all courses with topic/subtopic counts |
| GET | `/api/courses/{courseId}` | Get full course detail (topics, subtopics, content) |
| GET | `/api/search?q={query}` | Search across all course content |
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive JWT token |

### Authenticated (JWT Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/courses/{courseId}/enroll` | Enroll in a course |
| POST | `/api/subtopics/{subtopicId}/complete` | Mark subtopic as completed |
| GET | `/api/enrollments/{enrollmentId}/progress` | View enrollment progress |

### Error Responses

All errors return:
```json
{
  "error": "Error Type",
  "message": "Human-readable description",
  "timestamp": "2026-01-30T10:30:00Z"
}
```

| Status | When |
|--------|------|
| 400 | Validation failed (missing/invalid fields) |
| 401 | Missing or invalid JWT |
| 403 | Not enrolled in the course |
| 404 | Course, subtopic, or enrollment not found |
| 409 | Duplicate email or already enrolled |

---

## Authentication Flow

```
1. User registers
   POST /api/auth/register { email, password }
        │
        ▼
   Password hashed with BCrypt → saved to DB
        │
        ▼
   Response: { id, email, message }

2. User logs in
   POST /api/auth/login { email, password }
        │
        ▼
   AuthenticationManager verifies credentials
        │
        ▼
   JwtTokenProvider generates token (HS256, 24h expiry)
        │
        ▼
   Response: { token, email, expiresIn }

3. User makes authenticated request
   GET /api/enrollments/1/progress
   Authorization: Bearer <token>
        │
        ▼
   JwtAuthenticationFilter intercepts request
        │
        ▼
   Extracts token from "Authorization: Bearer ..." header
        │
        ▼
   Validates signature + expiration via JwtTokenProvider
        │
        ▼
   Loads UserDetails from DB via CustomUserDetailsService
        │
        ▼
   Sets SecurityContext → controller can access authenticated user
```

### Security Configuration

- **Stateless sessions** — no server-side session, every request carries its own JWT.
- **BCrypt** — password hashing with adaptive cost factor.
- **HS256** — JWT signed with HMAC-SHA256 using a 256-bit secret key.
- **24-hour expiry** — tokens expire after 86400000ms (configurable via `jwt.expiration`).

---

## Search Architecture

The search system has two layers with automatic fallback:

```
GET /api/search?q=velocity
         │
         ▼
   SearchService.search()
         │
         ├── Elasticsearch available?
         │         │
         │    YES  │  NO
         │         │
         ▼         ▼
   ES multi_match    PostgreSQL ILIKE
   query             fallback search
```

### Elasticsearch Search (Primary)

When ES is available, the search uses:

- **multi_match query** across 5 fields: courseTitle, courseDescription, topicTitle, subtopicTitle, content
- **Field boosting**: titles boosted ^3, description ^2, content ^1
- **Fuzziness: AUTO** — tolerates typos (e.g., "physcs" matches "physics")
- **Highlights** — returns snippets with matched terms wrapped in `<em>` tags
- Results are grouped by course

### PostgreSQL Fallback

When ES is not reachable:

- Loads all courses from the database
- Does case-insensitive `contains()` matching against all fields
- Extracts a snippet around the match position
- Groups results by course

### Indexing

On application startup:
1. SearchService pings Elasticsearch
2. If available → deletes and recreates the `course_content` index
3. Bulk-indexes every subtopic as a document with its parent course/topic metadata
4. If not available → logs a warning, search falls back to PostgreSQL

---

## Configuration & Profiles

### How Profiles Work

Spring Boot merges configuration files in layers:

```
application.yaml          (always loaded — shared base config)
         +
application-{profile}.yaml  (loaded on top — profile-specific overrides)
         =
Final effective configuration
```

### Config Files

**application.yaml** — Shared settings that apply everywhere:
```yaml
spring.jpa.hibernate.ddl-auto: update     # Auto-create/update tables
spring.jpa.open-in-view: false            # Prevent lazy loading in views
spring.datasource.driver-class-name       # PostgreSQL driver
jwt.secret                                # JWT signing key (from env var)
jwt.expiration: 86400000                  # 24 hours in milliseconds
springdoc.swagger-ui.path                 # Swagger UI URL
elasticsearch.url                         # ES connection URL
elasticsearch.apikey                      # ES API key (for Elastic Cloud)
```

**application-dev.yaml** — Local development:
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/courseplatform
spring.datasource.username: postgres
spring.datasource.password: 1234
spring.jpa.show-sql: true                 # Log SQL queries to console
elasticsearch.url: http://localhost:9200
```

**application-prod.yaml** — Production (reads from environment variables):
```yaml
spring.datasource.url: ${DATABASE_URL}
spring.datasource.username: ${DB_USERNAME:postgres}
spring.datasource.password: ${DB_PASSWORD:postgres}
spring.jpa.show-sql: false
elasticsearch.url: ${ELASTICSEARCH_URL:http://localhost:9200}
elasticsearch.apikey: ${ELASTICSEARCH_APIKEY:}
```

### Activating a Profile

```bash
# Via environment variable
SPRING_PROFILES_ACTIVE=dev

# Via command line
java -jar app.jar --spring.profiles.active=prod

# Via Docker build arg
docker build --build-arg PROFILE=prod .
```

---

## Docker Setup

### Architecture

```
┌──────────────────── Docker Compose Network ────────────────────┐
│                                                                 │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐    │
│  │   postgres     │  │ elasticsearch  │  │      app       │    │
│  │  Port: 5432    │  │  Port: 9200    │  │  Port: 8080    │    │
│  │                │  │                │  │                │    │
│  │ Health check:  │  │ Health check:  │  │ Waits for both │    │
│  │ pg_isready     │  │ curl /_cluster │  │ to be healthy  │    │
│  │                │  │ /health        │  │                │    │
│  │ Volume:        │  │ Volume:        │  │ Built from     │    │
│  │ pgdata         │  │ esdata         │  │ Dockerfile     │    │
│  └────────────────┘  └────────────────┘  └────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Dockerfile — Two-Stage Build

**Stage 1 (Build):**
- Uses `maven:3.9-eclipse-temurin-17` (large image with Maven + JDK)
- Copies `pom.xml` first → runs `mvn dependency:go-offline` (caches dependencies)
- Copies source code → runs `mvn clean package`
- Produces a JAR file in `/app/target/`

**Stage 2 (Run):**
- Uses `eclipse-temurin:17-jre` (small image with just the JRE)
- Copies only the JAR from stage 1
- Sets the active Spring profile via `PROFILE` build arg
- Runs the JAR on port 8080

Why two stages? The build stage has Maven, source code, and build tools (~800MB). The run stage has only the JRE and JAR (~300MB). This keeps the final image small.

### docker-compose.yml — Three Services

**postgres:**
- Image: `postgres:16-alpine` (lightweight)
- Creates `courseplatform` database automatically
- Health check: `pg_isready` ensures PG is accepting connections
- Volume: `pgdata` persists data across restarts

**elasticsearch:**
- Image: `elasticsearch:8.17.0`
- Single-node mode (no cluster needed for dev)
- Security disabled (no username/password for local)
- 512MB heap (`-Xms512m -Xmx512m`)
- Health check: curls `/_cluster/health` — waits up to 30s for ES to boot
- Volume: `esdata` persists indexes

**app:**
- Built from the Dockerfile
- Profile set via `PROFILE` env var (defaults to `prod`)
- Connects to postgres and ES using container hostnames
- `depends_on` with `condition: service_healthy` — won't start until both dependencies are ready

### Running

```powershell
# Start everything (prod profile, default)
docker-compose up --build

# Start with dev profile
$env:PROFILE="dev"; docker-compose up --build

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (wipes data)
docker-compose down -v
```

---

## Seed Data

On startup, `DataLoader` (a `CommandLineRunner`) checks if the `courses` table is empty. If so, it reads `seed_data/courses.json` from the classpath and persists all courses, topics, and subtopics.

The seed data contains 3 courses:
- **physics-101** — Kinematics, Dynamics, Work and Energy (9 subtopics)
- **math-101** — Algebra, Functions, Calculus Intuition (9 subtopics)
- **cs-101** — Programming Basics, Algorithms, Data Structures (9 subtopics)

Each subtopic has detailed markdown content with formulas, examples, and tables.

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile |
| `DATABASE_URL` | Prod only | — | JDBC PostgreSQL URL |
| `DB_USERNAME` | No | `postgres` | Database username |
| `DB_PASSWORD` | No | `postgres` | Database password |
| `JWT_SECRET` | Recommended | dev fallback key | 256-bit secret for signing JWTs |
| `ELASTICSEARCH_URL` | No | `http://localhost:9200` | Elasticsearch endpoint |
| `ELASTICSEARCH_APIKEY` | No | (empty) | API key for Elastic Cloud |

## Lab #4 – REST API Blueprints (Java 21 / Spring Boot 3.3.x)
# Colombian School of Engineering – Software Architectures  


## 📋 Requirements
- Java 21
- Maven 3.9+

## ▶️ Project Execution
```bash
mvn clean install
mvn spring-boot:run
```
Test with `curl`:
```bash
curl -s http://localhost:8080/blueprints | jq
curl -s http://localhost:8080/blueprints/john | jq
curl -s http://localhost:8080/blueprints/john/house | jq
curl -i -X POST http://localhost:8080/blueprints -H 'Content-Type: application/json' -d '{ "author":"john","name":"kitchen","points":[{"x":1,"y":1},{"x":2,"y":2}] }'
curl -i -X PUT  http://localhost:8080/blueprints/john/kitchen/points -H 'Content-Type: application/json' -d '{ "x":3,"y":3 }'
```

> If you want to activate point filters (redundancy reduction, *undersampling*, etc.), implement new classes that implement `BlueprintsFilter` and replace them with `IdentityFilter` using `@Primary` or Spring configuration.

Open in browser:  
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)  


## 🗂️ Folder Structure (architecture)

```
src/main/java/edu/eci/arsw/blueprints
  ├── model/         # Domain entities: Blueprint, Point
  ├── persistence/   # Interface + repositories (InMemory, Postgres)
  │    └── impl/     # Concrete implementations
  ├── services/      # Business logic and orchestration
  ├── filters/       # Processing filters (Identity, Redundancy, Undersampling)
  ├── controllers/   # REST Controllers (BlueprintsAPIController)
  └── config/        # Configuration (Swagger/OpenAPI, etc.)
```

> This separation follows the **logical layers** pattern (model, persistence, services, controllers), facilitating extension to new technologies or data sources.

## 📖 Lab Activities

### 1. Familiarization with the Code Base

This project follows a **layered architecture** that cleanly separates domain models, persistence, business logic, and API controllers. Below is a detailed walkthrough of every component you should understand before extending the project.

---

#### 1.1 Model Layer (`model/`)

| Class | Type | Responsibility |
|-------|------|----------------|
| `Blueprint` | Class | Represents a drawing, uniquely identified by `author` + `name`. Holds an internal `List<Point>`. Equality and `hashCode` are based solely on author and name. |
| `Point` | Record | Immutable 2-D coordinate `(int x, int y)`. Used to define the geometry of a blueprint. |

Key behaviors to note:
- `Blueprint.getPoints()` returns an **unmodifiable view** — you cannot mutate the list externally.
- `Blueprint.addPoint(Point p)` is the only way to append a point after construction.
- Two `Blueprint` objects are considered equal if they share the same `author` and `name`, regardless of their points.

---

#### 1.2 Persistence Layer (`persistence/`)

| Component | Role |
|-----------|------|
| `BlueprintPersistence` | Interface defining the storage contract (save, get by author/name, list all, add point). |
| `InMemoryBlueprintPersistence` | Concrete implementation using a thread-safe `ConcurrentHashMap`. Pre-loaded with sample blueprints for `john` and `jane`. |
| `BlueprintNotFoundException` | Thrown when a requested blueprint does not exist. |
| `BlueprintPersistenceException` | Thrown when a persistence constraint is violated (e.g., duplicate blueprint). |

The map key is `"author:name"` — a simple composite string that guarantees uniqueness.

Sample data loaded on startup:
```
john/house   → 4 points
john/garage  → 3 points
jane/garden  → 3 points
```

---

#### 1.3 Service Layer (`services/`)

`BlueprintsServices` is the **orchestration layer** between controllers and persistence. It:
- Delegates all CRUD operations to the injected `BlueprintPersistence` bean.
- Applies the injected `BlueprintsFilter` before returning blueprints to callers.
- Is annotated `@Service`, so Spring manages its lifecycle and injects its dependencies automatically.

> **Important:** Filters are only applied in `getBlueprint(author, name)` — not in `getAllBlueprints()` or `getBlueprintsByAuthor()`. Keep this in mind when extending the logic.

---

#### 1.4 Controller Layer (`controllers/`)

`BlueprintsAPIController` exposes the REST API at `/blueprints` and maps HTTP verbs to service calls:

| Method | Path | Action | Success Code |
|--------|------|--------|-------------|
| `GET` | `/blueprints` | List all blueprints | `200 OK` |
| `GET` | `/blueprints/{author}` | Blueprints by author | `200 OK` |
| `GET` | `/blueprints/{author}/{bpname}` | Single blueprint | `200 OK` |
| `POST` | `/blueprints` | Create new blueprint | `201 Created` |
| `PUT` | `/blueprints/{author}/{bpname}/points` | Add a point | `202 Accepted` |

The inner record `NewBlueprintRequest` acts as the **DTO** for POST requests, validated with `@NotBlank` and `@Valid`.

---

#### 1.5 Filters Layer (`filters/`)

| Filter | Profile | Behavior |
|--------|---------|----------|
| `IdentityFilter` | *(default)* | Returns the blueprint unchanged. |
| `RedundancyFilter` | `redundancy` | Removes consecutive duplicate points. |
| `UndersamplingFilter` | `undersampling` | Keeps only even-indexed points (1 out of every 2). |

Filters are activated via **Spring profiles**. Only one filter bean is active at a time. To switch filters, run with:
```bash
# Redundancy filter
mvn spring-boot:run -Dspring-boot.run.profiles=redundancy

# Undersampling filter
mvn spring-boot:run -Dspring-boot.run.profiles=undersampling
```

---

#### 1.6 Request / Response Flow

```
Client
  │
  ▼
BlueprintsAPIController        ← validates HTTP input, sets status codes
  │
  ▼
BlueprintsServices             ← orchestrates business logic, applies filter
  │
  ▼
BlueprintPersistence (impl)    ← reads/writes data store
  │
  ▼
In-Memory Map / PostgreSQL     ← actual storage
```

### 2. Migration to PostgreSQL persistence
- Set up a PostgreSQL database (you can use Docker).  
- Implement a new repository `PostgresBlueprintPersistence` to replace the in-memory version.  
- Maintain the contract of the `BlueprintPersistence` interface.  

### 3. REST API Best Practices
- Change the base path of controllers to `/api/v1/blueprints`.  
- Use correct **HTTP codes**:  
  - `200 OK` (successful queries).  
  - `201 Created` (creation).  
  - `202 Accepted` (updates).  
  - `400 Bad Request` (invalid data).  
  - `404 Not Found` (nonexistent resource).  
- Implement a generic uniform response class:
  ```java
  public record ApiResponse<T>(int code, String message, T data) {}
  ```
  JSON example:
  ```json
  {
    "code": 200,
    "message": "execute ok",
    "data": { "author": "john", "name": "house", "points": [...] }
  }
  ```

### 4. OpenAPI / Swagger
- Configure `springdoc-openapi` in the project.  
- Expose automatic documentation at `/swagger-ui.html`.  
- Annotate endpoints with `@Operation` and `@ApiResponse`.

### 5. *Blueprints* Filters
- Implement filters:
  - **RedundancyFilter**: removes consecutive duplicate points.  
  - **UndersamplingFilter**: keeps 1 out of every 2 points.  
- Activate filters using Spring profiles (`redundancy`, `undersampling`).  


## ✅ Deliverables

1. GitHub repository with:  
   - Updated source code.  
   - PostgreSQL configuration (`application.yml` or SQL script).  
   - Swagger/OpenAPI enabled.  
   - `ApiResponse<T>` class implemented.  

2. Documentation:  
   - Lab report with clear instructions.  
   - Evidence of queries in Swagger UI and evidence of messages in the database.  
   - Brief explanation of applied best practices.  


## 📊 Evaluation Criteria

| Criterion | Weight |
|----------|------|
| API design (versioning, DTOs, ApiResponse) | 25% |
| Migration to PostgreSQL (repository and correct persistence) | 25% |
| Correct use of HTTP codes and error handling | 20% |
| Documentation with OpenAPI/Swagger + README | 15% |
| Basic tests (unit or integration) | 15% |

**Bonus**:  

- Container image (`spring-boot:build-image`).  
- Metrics with Actuator.  
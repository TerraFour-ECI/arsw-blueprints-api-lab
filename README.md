## Lab #4 – REST API Blueprints (Java 21 / Spring Boot 3.3.x)
# Colombian School of Engineering – Software Architecture 


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
curl -s http://localhost:8080/api/v1/blueprints | jq
curl -s http://localhost:8080/api/v1/blueprints/john | jq
curl -s http://localhost:8080/api/v1/blueprints/john/house | jq
curl -i -X POST http://localhost:8080/api/v1/blueprints -H "Content-Type: application/json" -d "{\"author\":\"john\",\"name\":\"kitchen\",\"points\":[{\"x\":1,\"y\":1},{\"x\":2,\"y\":2}]}"
curl -i -X PUT  http://localhost:8080/api/v1/blueprints/john/kitchen/points -H "Content-Type: application/json" -d "{\"x\":3,\"y\":3}"
```

> To activate point filters (redundancy reduction, undersampling), run with the corresponding Spring profile. See [Section 5.6](#56-activating-filters-via-spring-profiles).

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

> **Note:** Filters are applied in all retrieval methods: `getAllBlueprints()`, `getBlueprintsByAuthor()`, and `getBlueprint(author, name)`. See [Section 5.5](#55-filter-integration-in-the-service-layer) for details.

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
mvn spring-boot:run "-Dspring-boot.run.profiles=redundancy"

# Undersampling filter
mvn spring-boot:run "-Dspring-boot.run.profiles=undersampling"
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

### 2. Migration to PostgreSQL Persistence

This section describes how the persistence layer was migrated from in-memory storage (`InMemoryBlueprintPersistence`) to a real PostgreSQL database, while maintaining the `BlueprintPersistence` interface contract without modifying any other layer of the project.

---

#### 2.1 Solution Architecture

The following components were added without touching the existing code:

```
persistence/
  ├── entity/
  │    ├── BlueprintEntity.java        ← JPA entity for the blueprints table
  │    └── PointEntity.java            ← JPA entity for the points table
  ├── jpa/
  │    └── BlueprintJpaRepository.java ← Spring Data JPA (auto-generated queries)
  └── impl/
       └── PostgresBlueprintPersistence.java ← Implements BlueprintPersistence
```

The Postgres implementation is only activated with the `postgres` Spring profile (`@Profile("postgres")`) and is annotated with `@Primary` to override `InMemoryBlueprintPersistence` when that profile is active.

---

#### 2.2 Dependencies Added to `pom.xml`

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>runtime</scope>
</dependency>
```

---

#### 2.3 Database Configuration

The file `src/main/resources/application-postgres.properties` was created (only active with the `postgres` profile):

```properties
# DataSource
spring.datasource.url=jdbc:postgresql://localhost:5432/blueprints
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Seed data
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
```

> **Note:** `ddl-auto=update` lets Hibernate create/update tables automatically on startup. No SQL script needs to be run manually.

---

#### 2.4 Starting PostgreSQL with Docker

**Prerequisite:** have [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

```bash
docker run --name blueprints-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=blueprints \
  -p 5432:5432 \
  -d postgres:16
```

Verify the container is running:

```bash
docker ps
```

You should see `blueprints-db` with status `Up`.

> **⚠️ Common issue on Windows:** if port 5432 is already in use by a local PostgreSQL installation, stop that service before running Docker, or change the container port to `-p 5433:5432` and update the URL in `application-postgres.properties` to `jdbc:postgresql://localhost:5433/blueprints`.

---

#### 2.5 Running the Application with the `postgres` Profile

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

On successful startup you will see in the logs:

```
HikariPool-1 - Start completed.
Hibernate: create table if not exists blueprints ...
Hibernate: create table if not exists points ...
Tomcat started on port 8080 (http)
```

---

#### 2.6 Verifying Data in the Database

Connect to the container and query the tables:

```bash
docker exec -it blueprints-db psql -U postgres -d blueprints
```

```sql
-- List created tables
\dt

-- View blueprints
SELECT * FROM blueprints;

-- View blueprints with their points
SELECT b.author, b.name, p.x, p.y, p.position
FROM blueprints b JOIN points p ON p.blueprint_id = b.id
ORDER BY b.author, b.name, p.position;
```

Expected output:

```
 author | name  | x  | y  | position
--------+-------+----+----+----------
 john   | house |  0 |  0 |        0
 john   | house | 10 |  0 |        1
 john   | house | 10 | 10 |        2
 john   | house |  0 | 10 |        3
```

---

#### 2.7 Auto-Generated Relational Model

Hibernate automatically generates two tables:

**`blueprints`**
| Column | Type    | Description                  |
|--------|---------|------------------------------|
| id     | BIGINT  | Auto-incremental PK          |
| author | VARCHAR | Blueprint author             |
| name   | VARCHAR | Blueprint name               |

With a UNIQUE constraint on `(author, name)`.

**`points`**
| Column       | Type   | Description                        |
|--------------|--------|------------------------------------|
| id           | BIGINT | Auto-incremental PK                |
| x            | INT    | X coordinate                       |
| y            | INT    | Y coordinate                       |
| position     | INT    | Point order within the blueprint   |
| blueprint_id | BIGINT | FK → blueprints(id)                |

---

#### 2.8 Running Without a Database (InMemory mode)

If Docker is not available or you want to run the app without PostgreSQL, simply run without any profile. Spring will automatically use `InMemoryBlueprintPersistence`:

```bash
mvn spring-boot:run
```

---

#### 2.9 Stopping and Restarting the Container

```bash
# Stop the container
docker stop blueprints-db

# Restart it (data is preserved)
docker start blueprints-db

# Remove it completely (deletes all data)
docker rm -f blueprints-db
```
  
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


---

#### 4.1 Configuration

Added the following dependency to `pom.xml`:

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

Created `OpenApiConfig.java` to customize the API documentation:

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
            .info(new Info()
                .title("ARSW Blueprints API")
                .version("v1")
                .description("REST API for blueprint management")
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Development Server")
            ));
    }
}
```

---

#### 4.2 Endpoint Annotations

All endpoints in `BlueprintsAPIController` were annotated with OpenAPI metadata:

- `@Operation`: Describes the endpoint's purpose and behavior
- `@ApiResponses`: Documents all possible HTTP response codes
- `@Parameter`: Describes path/query parameters
- `@Schema`: Provides examples and validation rules for request/response bodies

**Example:**

```java
@Operation(
    summary = "Get all blueprints",
    description = "Returns the complete list of blueprints stored in the system"
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200",
        description = "Blueprint list successfully retrieved"
    )
})
@GetMapping
public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
    // ...
}
```

---

#### 4.3 Accessing Swagger UI

Once the application is running, access the interactive documentation at:

🔗 **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)


![Swagger](images/swagger.png)

---

#### 4.4 Testing Endpoints via Swagger UI

Below are examples of testing each endpoint through the Swagger interface, along with database verification.

##### 4.4.1 GET `/api/v1/blueprints` - Get All Blueprints

**Swagger UI Interface:**

![GET All Blueprints - Swagger](images/swagger-get-all.png)
*Testing the getAllBlueprints endpoint*

**Response:**

![GET All Blueprints - Response](images/swagger-get-all-response.png)
*Successful response showing all blueprints*

**Database Verification:**

![Database - All Blueprints](images/db-all-blueprints.png)
*PostgreSQL query confirming data persistence*

```sql
SELECT * FROM blueprints;
```

---

##### 4.4.2 GET `/api/v1/blueprints/{author}` - Get Blueprints by Author

**Swagger UI Interface:**

![GET By Author - Swagger](images/swagger-get-by-author.png)
*Testing with author parameter "jane"*

**Response:**

![GET By Author - Response](images/swagger-get-by-author-response.png)
*Filtered blueprints for the specified author*

**Database Verification:**

![Database - By Author](images/db-by-author.png)
*PostgreSQL query filtering by author*

```sql
SELECT * FROM blueprints WHERE author = 'jane';
```

---

##### 4.4.3 GET `/api/v1/blueprints/{author}/{bpname}` - Get Specific Blueprint

**Swagger UI Interface:**

![GET Specific - Swagger](images/swagger-get-specific.png)
*Testing with author "john" and blueprint "house"*

**Response:**

![GET Specific - Response](images/swagger-get-specific-response.png)
*Complete blueprint details including all points*

**Database Verification:**

![Database - Specific Blueprint](images/db-specific.png)
*PostgreSQL query with JOIN to show blueprint and its points*

```sql
SELECT b.author, b.name, p.x, p.y, p.position
FROM blueprints b 
JOIN points p ON p.blueprint_id = b.id
WHERE b.author = 'john' AND b.name = 'house'
ORDER BY p.position;
```

---

##### 4.4.4 POST `/api/v1/blueprints` - Create New Blueprint

**Swagger UI Interface:**

![POST Create - Swagger](images/swagger-post-create.png)
*Creating a new blueprint with request body*

**Request Body Example:**

```json
{
  "author": "alice",
  "name": "mansion",
  "points": [
    {"x": 0, "y": 0},
    {"x": 100, "y": 0},
    {"x": 100, "y": 100},
    {"x": 0, "y": 100}
  ]
}
```

**Response:**

![POST Create - Response](images/swagger-post-response.png)
*HTTP 201 Created with the created blueprint*

**Database Verification:**

![Database - After POST](images/db-after-post.png)
*New blueprint and points inserted into PostgreSQL*

```sql
-- Verify blueprint was created
SELECT * FROM blueprints WHERE author = 'alice' AND name = 'mansion';

-- Verify points were saved
SELECT p.* FROM points p
JOIN blueprints b ON p.blueprint_id = b.id
WHERE b.author = 'alice' AND b.name = 'mansion'
ORDER BY p.position;
```

---

##### 4.4.5 PUT `/api/v1/blueprints/{author}/{bpname}/points` - Add Point

**Swagger UI Interface:**

![PUT Add Point - Swagger](images/put-add-point.png)
*Adding a new point to an existing blueprint*

**Request Body Example:**

```json
{
  "x": 50,
  "y": 50
}
```

**Response:**

![PUT Add Point - Response](images/swagger-put-response.png)
*HTTP 202 Accepted confirming point was added*


**Database Verification:**

![Database - After PUT](images/after-put.png)
*New point successfully added*

```sql
SELECT p.x, p.y, p.position 
FROM points p
JOIN blueprints b ON p.blueprint_id = b.id
WHERE b.author = 'alice' AND b.name = 'mansion'
ORDER BY p.position;
```

---

#### 4.5 Error Handling Examples

##### 4.5.1 Blueprint Not Found (404)

**Swagger UI:**

![404 Error - Swagger](images/swagger-404.png)
*Attempting to retrieve a non-existent blueprint*

**Response:**

```json
{
  "code": 404,
  "message": "Blueprint not found: nonexistent/blueprint",
  "data": null
}
```

---

##### 4.5.2 Duplicate Blueprint (403)

**Swagger UI:**

![403 Error - Swagger](images/swagger-403.png)
*Attempting to create a blueprint that already exists*

**Response:**

```json
{
  "code": 403,
  "message": "Blueprint already exists: john/house",
  "data": null
}
```

---

##### 4.5.3 Invalid Request (400)

**Swagger UI:**

![400 Error - Swagger](images/swagger-400.png)
*Sending a request with missing required fields*

**Response:**

```json
{
  "code": 400,
  "message": "Validation failed",
  "data": null
}
```

---


### 5. *Blueprints* Filters

This section describes the filter system that processes blueprints before returning them to the client. Filters reduce the size or clean the data of blueprints by transforming their point lists.

---

#### 5.1 Solution Architecture

The filter layer follows a **Strategy pattern** via a common interface:

```
filters/
  ├── BlueprintsFilter.java        ← Interface defining the filter contract
  ├── IdentityFilter.java          ← Default: no transformation
  ├── RedundancyFilter.java        ← Removes consecutive duplicate points
  └── UndersamplingFilter.java     ← Keeps 1 out of every 2 points
```

Only **one filter** is active at a time. The active filter is determined by the Spring profile. If no profile is set, `IdentityFilter` is used by default.

---

#### 5.2 Filter Interface

```java
public interface BlueprintsFilter {
    Blueprint apply(Blueprint bp);
}
```

Every filter receives a `Blueprint` and returns a **new** `Blueprint` with the transformed point list (or the same one, in the case of `IdentityFilter`).

---

#### 5.3 Available Filters

| Filter | Class | Profile | Behavior |
|--------|-------|---------|----------|
| Identity | `IdentityFilter` | *(default — no profile needed)* | Returns the blueprint unchanged. Acts as the baseline. |
| Redundancy | `RedundancyFilter` | `redundancy` | Removes **consecutive duplicate points**. If `(0,0), (0,0), (1,1)` → result is `(0,0), (1,1)`. Non-consecutive duplicates are preserved. |
| Undersampling | `UndersamplingFilter` | `undersampling` | Keeps only **even-indexed points** (indices 0, 2, 4, …). Effectively discards 1 out of every 2 points. Blueprints with ≤ 2 points are returned unchanged. |

---

#### 5.4 Implementation Details

##### 5.4.1 IdentityFilter

```java
@Component
@Profile("!redundancy & !undersampling")
public class IdentityFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) { return bp; }
}
```

- Active **only** when neither `redundancy` nor `undersampling` profiles are set.
- This avoids a bean conflict: without the `@Profile` exclusion, Spring would find two `BlueprintsFilter` beans and fail to start.

##### 5.4.2 RedundancyFilter

```java
@Component
@Profile("redundancy")
public class RedundancyFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) {
        List<Point> in = bp.getPoints();
        if (in.isEmpty()) return bp;
        List<Point> out = new ArrayList<>();
        Point prev = null;
        for (Point p : in) {
            if (prev == null || !(prev.x()==p.x() && prev.y()==p.y())) {
                out.add(p);
                prev = p;
            }
        }
        return new Blueprint(bp.getAuthor(), bp.getName(), out);
    }
}
```

- Iterates through the point list sequentially, comparing each point with the previous one.
- Only adds the point to the output if it differs from the previous one.
- Returns a **new** `Blueprint` with the cleaned list.

**Example:**

```
Input:  (0,0), (0,0), (1,1), (1,1), (2,2)
Output: (0,0), (1,1), (2,2)             → 5 points reduced to 3
```

##### 5.4.3 UndersamplingFilter

```java
@Component
@Profile("undersampling")
public class UndersamplingFilter implements BlueprintsFilter {
    @Override
    public Blueprint apply(Blueprint bp) {
        List<Point> in = bp.getPoints();
        if (in.size() <= 2) return bp;
        List<Point> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            if (i % 2 == 0) out.add(in.get(i));
        }
        return new Blueprint(bp.getAuthor(), bp.getName(), out);
    }
}
```

- Keeps only points at even indices (0, 2, 4, …), effectively halving the point density.
- Blueprints with 2 or fewer points are returned unchanged to avoid losing too much data.

**Example:**

```
Input:  (0,0), (1,1), (2,2), (3,3), (4,4)
Output: (0,0), (2,2), (4,4)              → 5 points reduced to 3
```

---

#### 5.5 Filter Integration in the Service Layer

Filters are applied in `BlueprintsServices` across **all three retrieval methods**, ensuring consistent behavior regardless of how blueprints are queried:

```java
@Service
public class BlueprintsServices {

    private final BlueprintPersistence persistence;
    private final BlueprintsFilter filter;

    public Set<Blueprint> getAllBlueprints() {
        return persistence.getAllBlueprints().stream()
                .map(filter::apply)
                .collect(Collectors.toSet());
    }

    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        return persistence.getBlueprintsByAuthor(author).stream()
                .map(filter::apply)
                .collect(Collectors.toSet());
    }

    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return filter.apply(persistence.getBlueprint(author, name));
    }
}
```

| Method | Filter Applied |
|--------|---------------|
| `getAllBlueprints()` |  Yes — to every blueprint in the set |
| `getBlueprintsByAuthor()` |  Yes — to every blueprint in the set |
| `getBlueprint()` |  Yes — to the single blueprint returned |

> **Note:** `addNewBlueprint()` and `addPoint()` do **not** apply filters — they write raw data to persistence. Filters are only applied on **read** operations.

---

#### 5.6 Activating Filters via Spring Profiles

To switch the active filter, run the application with the corresponding Spring profile:

```bash
# Default (no filter — IdentityFilter)
mvn spring-boot:run

# Redundancy filter
mvn spring-boot:run "-Dspring-boot.run.profiles=redundancy"

# Undersampling filter
mvn spring-boot:run "-Dspring-boot.run.profiles=undersampling"
```

> **Windows PowerShell**: The `-D` argument **must** be enclosed in double quotes (`"..."`), otherwise PowerShell interprets it as its own parameter and Maven fails with `Unknown lifecycle phase`.

Profiles can also be combined with the `postgres` profile:

```bash
# PostgreSQL + Redundancy filter
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres,redundancy"

# PostgreSQL + Undersampling filter
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres,undersampling"
```

---

#### 5.7 Unit Tests

All filters are covered in `FiltersTest.java` with the following test cases:

| Test | Filter | Scenario | Expected |
|------|--------|----------|----------|
| `testIdentityFilter` | Identity | 3 distinct points | All 3 points returned unchanged |
| `testRedundancyFilter` | Redundancy | 5 points with consecutive duplicates | 3 unique consecutive points |
| `testRedundancyFilterNoConsecutiveDuplicates` | Redundancy | Non-consecutive duplicate `(0,0), (1,1), (0,0)` | All 3 points preserved (not consecutive) |
| `testUndersamplingFilter` | Undersampling | 5 points | 3 points (indices 0, 2, 4) |
| `testUndersamplingFilterSmallBlueprint` | Undersampling | 2 points only | Both points preserved (≤ 2 threshold) |

Run the filter tests:

```bash
mvn test -Dtest=FiltersTest
```

Expected output:

```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

#### 5.8 Evidence

---

##### 5.8.1 Default Behavior (IdentityFilter — No Profile)

Running without a profile uses `IdentityFilter`, which returns all points unchanged:

```bash
mvn spring-boot:run
```

**GET** `/api/v1/blueprints/john/house`:

![Identity Filter Response](images/filter-identity-response.png)
*All 4 original points returned without modification*

---

##### 5.8.2 RedundancyFilter Active

Running with the `redundancy` profile removes consecutive duplicate points:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=redundancy"
```

**GET** `/api/v1/blueprints/john/house`:

![Redundancy Filter Response](images/filter-redundancy-response.png)
*Consecutive duplicate points removed from the response*

---

##### 5.8.3 UndersamplingFilter Active

Running with the `undersampling` profile keeps only even-indexed points:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=undersampling"
```

**GET** `/api/v1/blueprints/john/house`:

![Undersampling Filter Response](images/filter-undersampling-response.png)
*Only points at indices 0, 2 are returned (4 points → 2 points)*

---


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
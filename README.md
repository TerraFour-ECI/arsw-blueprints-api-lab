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

### 1. Familiarization with the code base

This project follows a layered architecture, separating domain models, persistence, business logic, and API controllers for clarity and maintainability. Below is a summary of the main components:


---

**Model Layer (`model` package):**
- `Blueprint`: Represents a blueprint drawing, uniquely identified by an `author` and a `name`. It contains a list of `Point` objects that define the shape. The class provides methods to retrieve its properties and to add new points. Equality and hash code are based on the author and name, ensuring uniqueness.
- `Point`: An immutable record for a 2D point with integer coordinates `x` and `y`. Used to define the geometry of a blueprint.

**Persistence Layer (`persistence` package):**
- `BlueprintPersistence` (interface): Defines the contract for storing and retrieving blueprints, including methods for saving, fetching by author or name, listing all blueprints, and adding points to an existing blueprint.
- `InMemoryBlueprintPersistence`: Implements the interface using a thread-safe map for fast, in-memory storage. It initializes with sample blueprints for demonstration and testing. All CRUD operations are supported, and errors (such as duplicate or missing blueprints) are handled with custom exceptions.
- `BlueprintPersistenceException` and `BlueprintNotFoundException`: Custom exceptions to signal errors in persistence operations, such as trying to add a duplicate blueprint or requesting a non-existent one.

**Service Layer (`services` package):**
- `BlueprintsServices`: Central business logic layer. It receives requests from controllers, delegates data operations to the persistence layer, and applies any configured filters to blueprints before returning them. This class is annotated as a Spring `@Service` for automatic dependency injection. It ensures that business rules (such as filtering) are consistently applied.

**Controller Layer (`controllers` package):**
- `BlueprintsAPIController`: The REST API entry point. It exposes endpoints to:
  - List all blueprints (`GET /blueprints`)
  - Get blueprints by author (`GET /blueprints/{author}`)
  - Get a specific blueprint by author and name (`GET /blueprints/{author}/{bpname}`)
  - Create a new blueprint (`POST /blueprints`)
  - Add a point to an existing blueprint (`PUT /blueprints/{author}/{bpname}/points`)
  The controller uses standard HTTP status codes, validates input, and handles exceptions to provide clear API responses. It is annotated with `@RestController` and uses Spring's mapping annotations for routing.

**Filters Layer (`filters` package):**
- `BlueprintsFilter` (interface): Defines a contract for processing blueprints (e.g., removing redundant points).
- `IdentityFilter`: Default filter that returns the blueprint unchanged. Other filters (like redundancy or undersampling) can be implemented and injected as needed.

---

**Class Relationships and Flow Example:**
1. A client sends a request to the API (e.g., to add a new blueprint).
2. `BlueprintsAPIController` receives the request, validates the input, and calls the appropriate method in `BlueprintsServices`.
3. `BlueprintsServices` delegates data operations to the persistence layer (`BlueprintPersistence`), and applies any filters if needed.
4. The persistence implementation (e.g., `InMemoryBlueprintPersistence`) performs the requested operation and returns the result or throws an exception if there is an error.
5. The controller formats the response, setting the correct HTTP status and body, and returns it to the client.

**Design Highlights:**
- **Separation of concerns:** Each layer has a single responsibility, making the codebase modular and easy to maintain.
- **Extensibility:** New persistence mechanisms (e.g., PostgreSQL), filters, or business rules can be added with minimal changes to existing code.
- **Testability:** Interfaces and dependency injection allow for easy mocking and unit testing of each layer.
- **Robust error handling:** Custom exceptions and HTTP status codes provide clear feedback to API consumers.

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
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
- Review the `model` package with the `Blueprint` and `Point` classes.  
- Understand the `persistence` layer with `InMemoryBlueprintPersistence`.  
- Analyze the `services` layer (`BlueprintsServices`) and the `BlueprintsAPIController` controller.

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
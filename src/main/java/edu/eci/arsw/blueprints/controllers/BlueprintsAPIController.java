package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.controllers.dto.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "REST API for blueprint management")

public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }


    @Operation(
    summary = "Get all blueprints",
    description = "Returns the complete list of blueprints stored in the system"
    )
    @ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Blueprint list successfully retrieved",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        Set<Blueprint> blueprints = services.getAllBlueprints();
        return ResponseEntity.ok(
                new ApiResponse<>(200, "execute ok", blueprints));
    }

    @Operation(
    summary = "Get blueprints by author",
    description = "Returns all blueprints created by a specific author"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Author's blueprints successfully retrieved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No blueprints found for the specified author"
        )
    })
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<?>> byAuthor(
        @Parameter(description = "Author name", example = "John") 
        @PathVariable String author) {
        try {
            Set<Blueprint> blueprints = services.getBlueprintsByAuthor(author);
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "execute ok", blueprints));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    @Operation(
    summary = "Get a specific blueprint",
    description = "Returns a blueprint identified by its author and name. Applies the configured filter."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Blueprint successfully retrieved"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Blueprint not found"
        )
    })
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<?>> byAuthorAndName(
            @Parameter(description = "Author name", example = "john") 
            @PathVariable String author, 
            @Parameter(description = "Blueprint name", example = "house") 
            @PathVariable String bpname) {
        try {
            Blueprint BP = services.getBlueprint(author, bpname);   
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "execute ok", BP));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    @Operation(
    summary = "Create a new blueprint",
    description = "Registers a new blueprint in the system. The blueprint must have a unique author and name."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Blueprint successfully created"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Blueprint already exists (duplicate)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input data (missing author/name or invalid points)"
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<?>> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "blueprint created", bp));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, e.getMessage(), null));
        }
    }


    @Operation(
    summary = "Add a point to a blueprint",
    description = "Adds a new point (x, y coordinate) to an existing blueprint"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "202",
            description = "Point successfully added"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Blueprint not found"
        )
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<?>> addPoint(
        @Parameter(description = "Author name", example = "john") 
            @PathVariable String author, 
            @Parameter(description = "Blueprint name", example = "house") 
            @PathVariable String bpname,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Point to add (x, y coordinates)",
                content = @Content(schema = @Schema(implementation = Point.class))
            )
            @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ApiResponse<>(202, "point added", null));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null ));
        }
    }

    @Schema(description = "DTO for creating a new blueprint")
    public record NewBlueprintRequest(

            @NotBlank
            @Schema(description = "Blueprint author", example = "john")
            String author,

            @NotBlank
            @Schema(description = "Blueprint name", example = "kitchen")
            String name,

            @Valid
            @Schema(description = "List of blueprint points (x, y)")
            java.util.List<Point> points

    ) { }
}

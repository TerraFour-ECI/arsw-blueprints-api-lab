package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BlueprintControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlueprintsServices services;

    @Test
    void testGetAllBlueprints() throws Exception {
        mockMvc.perform(get("/api/v1/blueprints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("execute ok"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetBlueprintsByAuthor() throws Exception {
        mockMvc.perform(get("/api/v1/blueprints/john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetBlueprintsByAuthorNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/blueprints/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void testGetBlueprint() throws Exception {
        mockMvc.perform(get("/api/v1/blueprints/john/house"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.author").value("john"))
                .andExpect(jsonPath("$.data.name").value("house"));
    }

    @Test
    void testGetBlueprintNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/blueprints/john/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void testCreateBlueprint() throws Exception {
        String requestBody = """
        {
            "author": "bob",
            "name": "sketch",
            "points": [
                {"x": 1, "y": 1},
                {"x": 2, "y": 2}
            ]
        }
        """;

        mockMvc.perform(post("/api/v1/blueprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("blueprint created"));
    }

    @Test
    void testCreateBlueprintDuplicate() throws Exception {
        String requestBody = """
        {
            "author": "john",
            "name": "house",
            "points": []
        }
        """;

        mockMvc.perform(post("/api/v1/blueprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void testCreateBlueprintMissingAuthor() throws Exception {
        String requestBody = """
        {
            "name": "test",
            "points": []
        }
        """;

        mockMvc.perform(post("/api/v1/blueprints")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddPoint() throws Exception {
        String requestBody = """
        {
            "x": 99,
            "y": 99
        }
        """;

        mockMvc.perform(put("/api/v1/blueprints/john/house/points")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.message").value("point added"));
    }

    @Test
    void testAddPointBlueprintNotFound() throws Exception {
        String requestBody = """
        {
            "x": 1,
            "y": 1
        }
        """;

        mockMvc.perform(put("/api/v1/blueprints/nonexistent/blueprint/points")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}

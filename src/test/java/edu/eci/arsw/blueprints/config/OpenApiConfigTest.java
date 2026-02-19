package edu.eci.arsw.blueprints.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void testApiBeanIsCreated() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI api = config.api();

        assertNotNull(api);
        assertNotNull(api.getInfo());
        assertEquals("ARSW Blueprints API", api.getInfo().getTitle());
        assertEquals("v1", api.getInfo().getVersion());
    }

    @Test
    void testApiHasServer() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI api = config.api();

        assertNotNull(api.getServers());
        assertFalse(api.getServers().isEmpty());
        assertEquals("http://localhost:8080", api.getServers().get(0).getUrl());
    }
}
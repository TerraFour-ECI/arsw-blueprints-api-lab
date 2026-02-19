package edu.eci.arsw.blueprints.config;

import java.util.List;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("ARSW Blueprints API")
                        .version("v1")
                        .description("""
                                REST API para gestión de planos (blueprints).
                                
                                **Características:**
                                - Persistencia en memoria o PostgreSQL
                                - Filtros configurables (redundancia, undersampling)
                                - Validación de datos con Bean Validation
                                - Respuestas uniformes con ApiResponse<T>
                                """)
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server")
                ));
    }
}


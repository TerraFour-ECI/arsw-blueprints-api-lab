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
                                REST API for blueprint management.
                                
                                **Features:**
                                - In-memory or PostgreSQL persistence
                                - Configurable filters (redundancy, undersampling)
                                - Data validation with Bean Validation
                                - Uniform responses using ApiResponse<T>
                                """)
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server")
                ));
    }

}


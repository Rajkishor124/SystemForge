package com.systemforge.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3 configuration for SystemForge.
 *
 * <p>Accessible at:
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html}</li>
 *   <li>API Docs JSON: {@code /api-docs}</li>
 * </ul>
 *
 * <p>JWT Bearer auth is pre-configured in the UI so developers can
 * authenticate and test protected endpoints without any external tooling.
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_SCHEME_NAME = "BearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI systemForgeOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, bearerAuthScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("SystemForge API")
                .description("""
                        **SystemForge** — Enterprise Backend System Design Builder.
                        
                        Design, configure, and generate production-ready backend architectures.
                        
                        ### Authentication
                        Use the **Authorize** button to set your JWT Bearer token.
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("SystemForge Team")
                        .email("dev@systemforge.io"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://systemforge.io/terms"));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development"),
                new Server()
                        .url("https://api.staging.systemforge.io")
                        .description("Staging"),
                new Server()
                        .url("https://api.systemforge.io")
                        .description("Production")
        );
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name(BEARER_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT access token. Obtain it via POST /api/v1/auth/login");
    }
}
package com.studyai.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger avec support de l'authentification Bearer JWT.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "StudyAI API",
        version = "1.0.0",
        description = "API de la plateforme éducative StudyAI"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SwaggerConfig {}

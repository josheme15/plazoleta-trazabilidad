package com.plazoleta.trazabilidad.infrastructure.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Plazoleta - Microservicio de Trazabilidad",
        version = "1.0.0",
        description = "Registro y consulta del historial de cambios de estado de los pedidos (MongoDB). "
                + "El token se obtiene en POST /auth/login del microservicio de usuarios."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}

package com.lottery.audit.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Audit Service API", version = "1.0",
                description = "Append-only audit log. Events are written by other "
                        + "services with an internal key and read by ADMIN or AUDITOR users"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT",
        description = "User token from auth-service. Needed for reading events")
@SecurityScheme(name = "internalKey", type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER, paramName = "X-Internal-Key",
        description = "Shared secret used by other services. Needed for writing events")
public class OpenApiConfig {}
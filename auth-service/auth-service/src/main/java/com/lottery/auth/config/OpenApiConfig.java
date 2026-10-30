package com.lottery.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Auth Service API", version = "1.0",
                description = "Registration, login and JWT issuing"))
public class OpenApiConfig {}
package app.usfit.api.config;

import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI openAPI() {
        var bearer = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .name("Authorization")
            .in(SecurityScheme.In.HEADER);

        return new OpenAPI()
            .info(new Info().title("UsFit API").version("v1"))
            .components(new Components().addSecuritySchemes("bearerAuth", bearer))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server().url("https://api.usfit.kr")
                ));
    }
}

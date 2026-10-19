package io.github.elr.blog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API documentation metadata (issue #14). Operations that need a token reference the scheme with
 * {@code @SecurityRequirement(name = OpenApiConfig.BEARER)} so public endpoints show no lock icon.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI blogOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        String version = build != null ? build.getVersion() : "dev";
        return new OpenAPI()
                .info(new Info()
                        .title("Blog API")
                        .version(version)
                        .description("REST API for a technical blog. Errors are RFC 9457 problem details "
                                + "(application/problem+json); see docs/problems.md.")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Access token from POST /api/v1/auth/login")));
    }
}

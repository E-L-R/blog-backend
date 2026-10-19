package io.github.elr.blog.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Framework 7 API versioning: the version is the 2nd path segment, {@code /api/v1/...}.
 *
 * <p>Only {@code /api/**} is versioned, so actuator, OpenAPI docs and Swagger UI are unaffected.
 * Controllers map {@code /api/{version}/...} and declare {@code version = "v1"}: both route, but
 * springdoc documents the declared string, so {@code "1"} would show {@code /api/1/...} (#15).
 */
@Configuration(proxyBeanMethods = false)
public class ApiVersioningConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .usePathSegment(1, path -> path.value().startsWith("/api/"))
                .addSupportedVersions("1")
                // Requests outside /api/** (actuator, docs, error pages) carry no version: don't demand one.
                .setVersionRequired(false);
    }
}

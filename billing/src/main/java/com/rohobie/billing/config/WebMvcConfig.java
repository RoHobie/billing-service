package com.rohobie.billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * WebMvcConfig
 *
 * Configures resource handlers to serve the React Single Page Application (SPA).
 * Forwards any non-API client routes to index.html to allow client-side routing.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // Do not route /api or /actuator or /h2-console to index.html
                        if (resourcePath.startsWith("api") || resourcePath.startsWith("actuator") || resourcePath.startsWith("h2-console")) {
                            return null;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }
}

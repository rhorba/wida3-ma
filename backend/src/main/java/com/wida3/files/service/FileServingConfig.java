package com.wida3.files.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded photos directly from Spring for local dev/test. In the docker-compose
 * deployment this is superseded by Nginx serving the same volume (Architecture ADR-6).
 */
@Configuration
public class FileServingConfig implements WebMvcConfigurer {

    private final String storagePath;

    public FileServingConfig(@Value("${app.file-storage.path}") String storagePath) {
        this.storagePath = storagePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalizedLocation());
    }

    private String normalizedLocation() {
        String path = storagePath.replace('\\', '/');
        return path.endsWith("/") ? path : path + "/";
    }
}

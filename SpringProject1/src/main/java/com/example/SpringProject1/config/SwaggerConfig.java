package com.example.SpringProject1.config;

import org.springframework.context.annotation.Bean;
import org.springdoc.core.models.GroupedOpenApi;

public class SwaggerConfig {
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("springshop-public")
                .pathsToMatch("/**")
                .build();
    }
}
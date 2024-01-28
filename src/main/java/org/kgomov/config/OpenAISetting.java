package org.kgomov.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "settings.openai")
public record OpenAISetting(String apiKey) {
}

package com.example.providers.configuration;

import com.example.providers.configuration.props.ProvidersProperties;
import com.example.providers.source.ProviderSource;
import com.example.providers.source.impl.JsonFileProviderSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация RestClient и json-источника поставщиков.
 */
@Configuration
@EnableConfigurationProperties(ProvidersProperties.class)
public class ProviderConfig {

    /**
     * Таймауты и фабрика соединений задаются в application.properties (spring.http.client.*),
     * Boot применит их сам — дублировать настройку здесь не нужно.
     */
    @Bean
    public RestClient providerRestClient(RestClientBuilderConfigurer configurer) {
        return configurer.configure(RestClient.builder()).build();
    }

    @Bean
    public ProviderSource jsonFileProviderSource(ProvidersProperties properties,
                                                 ResourceLoader resourceLoader,
                                                 ObjectMapper objectMapper) {
        String location = properties.getJsonFile();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException(
                    "providers.json-file не задан: укажите путь к json-файлу поставщиков "
                            + "в формате file:... (чтение и изменение) или classpath:... (только чтение)");
        }
        return new JsonFileProviderSource(resourceLoader.getResource(location), objectMapper);
    }
}

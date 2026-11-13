package com.example.providers.configuration;

import com.example.providers.service.ProviderRegistry;
import com.example.providers.source.ProviderSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Создаёт реестр из настроенного источника поставщиков.
 */
@Configuration
public class RegistryConfig {

    @Bean
    public ProviderRegistry providerRegistry(ProviderSource providerSource) {
        return new ProviderRegistry(providerSource);
    }
}

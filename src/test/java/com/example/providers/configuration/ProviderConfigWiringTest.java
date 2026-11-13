package com.example.providers.configuration;

import com.example.providers.domain.Provider;
import com.example.providers.service.PhoneAvailabilityService;
import com.example.providers.service.ProviderRegistry;
import com.example.providers.source.impl.JsonFileProviderSource;
import com.example.providers.source.ProviderSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProviderConfigWiringTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private ProviderSource providerSource;

    @Autowired
    private ProviderRegistry registry;

    @Autowired
    private PhoneAvailabilityService service;

    @Autowired
    private ClientHttpRequestFactorySettings httpClientSettings;

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry properties) throws Exception {
        // Тест не зависит от providers.json из рабочей директории: набор пишется во временный файл.
        Path jsonFile = Files.writeString(tempDir.resolve("providers.json"), """
                [
                  {"id": "provider-a", "baseUrl": "https://a.example.com", "apiKey": "key-a"},
                  {"id": "provider-b", "baseUrl": "https://b.example.com", "apiKey": "key-b"}
                ]
                """);
        properties.add("providers.json-file", () -> jsonFile.toUri().toString());
    }

    @Test
    void shouldUseJsonFileSourceFromConfiguredLocation() {
        assertThat(providerSource).isInstanceOf(JsonFileProviderSource.class);
        assertThat(registry.findById("provider-a")).isPresent();
        assertThat(registry.findById("provider-b")).isPresent();
    }

    @Test
    void shouldExposeDomainProviders() {
        assertThat(registry.getAll())
                .extracting(Provider::id)
                .containsExactlyInAnyOrder("provider-a", "provider-b");
    }

    @Test
    void shouldWireService() {
        assertThat(service).isNotNull();
    }

    @Test
    void shouldApplyHttpTimeoutsFromProperties() {
        // Ридми обещает 5/10 секунд — проверяем, что в properties действительно так.
        assertThat(httpClientSettings.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(httpClientSettings.readTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
}

package com.example.providers.configuration;

import com.example.providers.domain.Provider;
import com.example.providers.service.ProviderRegistry;
import com.example.providers.source.impl.JsonFileProviderSource;
import com.example.providers.source.ProviderSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JsonSourceWiringTest {

    @TempDir
    static Path tempDir;

    @Autowired
    private ProviderSource providerSource;

    @Autowired
    private ProviderRegistry registry;

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry properties) throws Exception {
        String jsonLocation = Files.writeString(tempDir.resolve("providers.json"), """
                [{"id": "json-provider", "baseUrl": "https://j.example.com", "apiKey": "k"}]
                """).toUri().toString();
        properties.add("providers.json-file", () -> jsonLocation);
    }

    @Test
    void shouldLoadFromCustomFileAndPersistReplacement() throws Exception {
        assertThat(providerSource).isInstanceOf(JsonFileProviderSource.class);
        assertThat(registry.findById("json-provider")).isPresent();

        registry.replaceAll(List.of(
                new Provider("json-provider-2", "https://j2.example.com", "k2")));

        // Новый набор в памяти...
        assertThat(registry.findById("json-provider")).isEmpty();
        assertThat(registry.findById("json-provider-2")).isPresent();
        // ...и в файле.
        String fileContent = Files.readString(tempDir.resolve("providers.json"));
        assertThat(fileContent).contains("json-provider-2").doesNotContain("\"json-provider\"");
    }
}

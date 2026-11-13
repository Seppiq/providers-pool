package com.example.providers.source.impl;

import com.example.providers.domain.Provider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderSourceTest {

    @TempDir
    Path tempDir;

    private static final List<Provider> PROVIDERS_FROM_RESOURCES = List.of(
            new Provider("provider-a", "https://a.example.com", "key-a"),
            new Provider("provider-b", "https://b.example.com", "key-b"));

    @Test
    void shouldLoadProvidersFromJsonFileOnDisk() throws Exception {
        File file = Files.writeString(tempDir.resolve("providers.json"), """
                [
                  {"id": "provider-a", "baseUrl": "https://a.example.com", "apiKey": "key-a"},
                  {"id": "provider-b", "baseUrl": "https://b.example.com", "apiKey": "key-b"}
                ]
                """).toFile();

        JsonFileProviderSource source =
                new JsonFileProviderSource(new FileSystemResource(file), new ObjectMapper());

        assertThat(source.loadProviders()).containsExactlyElementsOf(PROVIDERS_FROM_RESOURCES);
    }

    @Test
    void shouldLoadProvidersFromClasspath() {
        JsonFileProviderSource source =
                new JsonFileProviderSource(new ClassPathResource("providers.json"), new ObjectMapper());

        assertThat(source.loadProviders()).containsExactlyElementsOf(PROVIDERS_FROM_RESOURCES);
    }

    @Test
    void jsonSourceShouldFailWithClearMessageWhenFileMissing() {
        JsonFileProviderSource source = new JsonFileProviderSource(
                new FileSystemResource(tempDir.resolve("missing.json").toFile()), new ObjectMapper());

        assertThatThrownBy(source::loadProviders)
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("missing.json");
    }

    @Test
    void shouldSaveProvidersToFileAndLoadThemBack() {
        File file = tempDir.resolve("providers.json").toFile();
        JsonFileProviderSource source =
                new JsonFileProviderSource(new FileSystemResource(file), new ObjectMapper());

        List<Provider> providers = List.of(
                new Provider("a", "https://a.example.com/", "key-a"),
                new Provider("b", "https://b.example.com", "key-b"));
        source.saveProviders(providers);

        assertThat(source.loadProviders()).containsExactlyElementsOf(providers);
        assertThat(file).exists();
    }

    @Test
    void shouldOverwritePreviousContentOnSave() throws Exception {
        File file = Files.writeString(tempDir.resolve("providers.json"), """
                [{"id": "old", "baseUrl": "https://old.example.com", "apiKey": "old"}]
                """).toFile();
        JsonFileProviderSource source =
                new JsonFileProviderSource(new FileSystemResource(file), new ObjectMapper());

        source.saveProviders(List.of(new Provider("new", "https://new.example.com", "key")));

        assertThat(source.loadProviders())
                .containsExactly(new Provider("new", "https://new.example.com", "key"));
    }

    @Test
    void saveShouldFailWithClearMessageForReadOnlyResource() {
        JsonFileProviderSource source =
                new JsonFileProviderSource(new ClassPathResource("providers.json"), new ObjectMapper());

        assertThatThrownBy(() -> source.saveProviders(List.of(
                new Provider("a", "https://a.example.com", "key-a"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("file:");
    }

    @Test
    void saveShouldNotLeaveTempFilesBehind() throws Exception {
        File file = tempDir.resolve("providers.json").toFile();
        JsonFileProviderSource source =
                new JsonFileProviderSource(new FileSystemResource(file), new ObjectMapper());

        source.saveProviders(List.of(new Provider("a", "https://a.example.com", "key-a")));

        // Атомарная запись через временный файл не оставляет мусора в директории.
        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files)
                    .extracting(Path::getFileName)
                    .containsExactly(Path.of("providers.json"));
        }
    }
}

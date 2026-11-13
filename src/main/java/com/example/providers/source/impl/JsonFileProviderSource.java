package com.example.providers.source.impl;

import com.example.providers.domain.Provider;
import com.example.providers.source.ProviderSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Источник поставщиков из json-файла.
 */
@RequiredArgsConstructor
public class JsonFileProviderSource implements ProviderSource {

    private final Resource resource;
    private final ObjectMapper objectMapper;

    @Override
    public List<Provider> loadProviders() {
        // Файл читается один раз при старте: битый файл роняет приложение сразу —
        // это честнее, чем загадочный сбой на первом запросе.
        try (InputStream in = resource.getInputStream()) {
            Provider[] providers = objectMapper.readValue(in, Provider[].class);
            return List.of(providers);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать файл поставщиков: " + resource, e);
        }
    }

    /**
     * Пишет атомарно: сначала во временный файл в той же директории,
     * затем переименовывает поверх целевого.
     */
    @Override
    public void saveProviders(List<Provider> providers) {
        if (!(resource instanceof WritableResource writable)) {
            throw new IllegalStateException(
                    "Набор поставщиков нельзя изменить: " + resource + " доступен только для чтения. "
                            + "Укажите файл на диске (file:...), чтобы редактировать набор.");
        }
        Path target = targetPath(writable);
        Path tmp = null;
        boolean moved = false;
        try {
            tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, providers);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить файл поставщиков: " + resource, e);
        } finally {
            if (!moved && tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException cleanupFailure) {
                    // не затираем основную ошибку неудачей очистки
                }
            }
        }
    }

    private static Path targetPath(WritableResource writable) {
        try {
            return writable.getFile().toPath().toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось определить файл поставщиков: " + writable, e);
        }
    }
}

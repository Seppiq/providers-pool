package com.example.providers.domain;

import java.util.Objects;

/**
 * Поставщик.
 */
public record Provider(String id, String baseUrl, String apiKey) {

    public Provider {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(apiKey, "apiKey");
        // Срезаем хвостовые слэши, иначе URL запроса получится с двойным слэшем.
        baseUrl = baseUrl.replaceAll("/+$", "");
    }
}

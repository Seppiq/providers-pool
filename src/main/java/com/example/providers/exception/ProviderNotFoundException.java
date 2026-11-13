package com.example.providers.exception;

/**
 * Поставщик с таким идентификатором не найден.
 */
public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String providerId) {
        super("Provider not found: " + providerId);
    }
}

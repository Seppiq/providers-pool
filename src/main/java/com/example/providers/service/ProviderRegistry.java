package com.example.providers.service;

import com.example.providers.domain.Provider;
import com.example.providers.source.ProviderSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр поставщиков по идентификатору.
 */
public class ProviderRegistry {

    private final ProviderSource source;

    /** Снимок набора; volatile — чтобы читатели видели замену целиком. */
    private volatile Map<String, Provider> providersById;

    public ProviderRegistry(ProviderSource source) {
        this.source = source;
        this.providersById = toMap(source.loadProviders());
    }

    /**
     * Заменяет весь набор: валидация, запись в источник, подмена в памяти.
     * Если валидация или запись не прошли, прежний набор остаётся на месте.
     */
    public synchronized void replaceAll(List<Provider> newProviders) {
        Map<String, Provider> newById = toMap(newProviders);
        source.saveProviders(newProviders);
        this.providersById = newById;
    }

    public Optional<Provider> findById(String providerId) {
        return Optional.ofNullable(providersById.get(providerId));
    }

    public List<Provider> getAll() {
        return List.copyOf(providersById.values());
    }

    private static Map<String, Provider> toMap(List<Provider> providers) {
        Map<String, Provider> byId = new HashMap<>();
        for (Provider provider : providers) {
            Provider previous = byId.putIfAbsent(provider.id(), provider);
            if (previous != null) {
                throw new IllegalStateException(
                        "Дублирующийся идентификатор поставщика: \"" + provider.id() + "\"");
            }
        }
        return Map.copyOf(byId);
    }
}

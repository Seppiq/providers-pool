package com.example.providers.source;

import com.example.providers.domain.Provider;

import java.util.List;

/**
 * Источник набора поставщиков.
 */
public interface ProviderSource {

    List<Provider> loadProviders();

    /** Сохраняет новый набор вместо прежнего. */
    void saveProviders(List<Provider> providers);
}

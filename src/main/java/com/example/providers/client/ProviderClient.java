package com.example.providers.client;

import com.example.providers.domain.Provider;

/**
 * Сам запрос к поставщику.
 */
public interface ProviderClient {

    /**
     * Проверяет, доступен ли номер телефона у поставщика.
     */
    boolean isPhoneAvailable(Provider provider, String phone);
}

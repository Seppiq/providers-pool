package com.example.providers.service;

import com.example.providers.domain.Provider;

/**
 * Проверка доступности номера телефона у поставщика.
 */
public interface PhoneAvailabilityService {

    /**
     * Возвращает, доступен ли номер у поставщика.
     *
     * @throws ProviderNotFoundException если поставщик не найден
     */
    boolean isPhoneAvailable(String providerId, String phone);
}

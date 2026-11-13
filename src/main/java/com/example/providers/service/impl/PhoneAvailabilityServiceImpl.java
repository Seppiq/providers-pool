package com.example.providers.service.impl;

import com.example.providers.client.ProviderClient;
import com.example.providers.domain.Provider;
import com.example.providers.exception.ProviderNotFoundException;
import com.example.providers.service.PhoneAvailabilityService;
import com.example.providers.service.ProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhoneAvailabilityServiceImpl implements PhoneAvailabilityService {

    private final ProviderRegistry registry;
    private final ProviderClient providerClient;

    @Override
    public boolean isPhoneAvailable(String providerId, String phone) {
        // Неизвестный id — исключение, а не false: тихий false маскирует баги вызывающего кода.
        Provider provider = registry.findById(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(providerId));
        return providerClient.isPhoneAvailable(provider, phone);
    }
}

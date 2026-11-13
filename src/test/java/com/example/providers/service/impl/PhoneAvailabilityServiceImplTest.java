package com.example.providers.service.impl;

import com.example.providers.client.ProviderClient;
import com.example.providers.domain.Provider;
import com.example.providers.exception.ProviderNotFoundException;
import com.example.providers.service.PhoneAvailabilityService;
import com.example.providers.service.ProviderRegistry;
import com.example.providers.source.ProviderSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhoneAvailabilityServiceImplTest {

    private static final Provider PROVIDER = new Provider("a", "https://a.example.com", "key-a");

    @Mock
    private ProviderClient providerClient;

    @Mock
    private ProviderSource providerSource;

    private PhoneAvailabilityService service;

    @BeforeEach
    void setUp() {
        when(providerSource.loadProviders()).thenReturn(List.of(PROVIDER));
        service = new PhoneAvailabilityServiceImpl(new ProviderRegistry(providerSource), providerClient);
    }

    @Test
    void shouldReturnAvailabilityForKnownProvider() {
        when(providerClient.isPhoneAvailable(PROVIDER, "79990001111")).thenReturn(true);

        assertThat(service.isPhoneAvailable("a", "79990001111")).isTrue();
    }

    @Test
    void shouldThrowForUnknownProvider() {
        assertThatThrownBy(() -> service.isPhoneAvailable("missing", "79990001111"))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void shouldPropagateProviderHttpErrorToCaller() {
        when(providerClient.isPhoneAvailable(PROVIDER, "79990001111"))
                .thenThrow(HttpServerErrorException.create(
                        org.springframework.http.HttpStatus.BAD_GATEWAY, "Bad Gateway", null, null, null));

        assertThatThrownBy(() -> service.isPhoneAvailable("a", "79990001111"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void shouldPropagateNetworkErrorToCaller() {
        when(providerClient.isPhoneAvailable(PROVIDER, "79990001111"))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> service.isPhoneAvailable("a", "79990001111"))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("connection refused");
    }
}

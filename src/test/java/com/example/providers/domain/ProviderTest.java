package com.example.providers.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderTest {

    @Test
    void shouldStripSingleTrailingSlashFromBaseUrl() {
        Provider provider = new Provider("a", "https://a.example.com/", "key-a");

        assertThat(provider.baseUrl()).isEqualTo("https://a.example.com");
    }

    @Test
    void shouldStripMultipleTrailingSlashesFromBaseUrl() {
        Provider provider = new Provider("a", "https://a.example.com///", "key-a");

        assertThat(provider.baseUrl()).isEqualTo("https://a.example.com");
    }

    @Test
    void shouldKeepBaseUrlWithoutTrailingSlash() {
        Provider provider = new Provider("a", "https://a.example.com", "key-a");

        assertThat(provider.baseUrl()).isEqualTo("https://a.example.com");
    }

    @Test
    void shouldRejectNullId() {
        assertThatThrownBy(() -> new Provider(null, "https://a.example.com", "key-a"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    void shouldRejectNullBaseUrl() {
        assertThatThrownBy(() -> new Provider("a", null, "key-a"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    void shouldRejectNullApiKey() {
        assertThatThrownBy(() -> new Provider("a", "https://a.example.com", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("apiKey");
    }
}

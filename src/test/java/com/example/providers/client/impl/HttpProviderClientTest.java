package com.example.providers.client.impl;

import com.example.providers.domain.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpProviderClientTest {

    private static final Provider PROVIDER = new Provider("p1", "https://api.example.com", "secret-key");
    private static final String PHONE = "79991234567";
    private static final String URL = "https://api.example.com/subscribers/" + PHONE;

    private MockRestServiceServer mockServer;
    private HttpProviderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new HttpProviderClient(builder.build());
    }

    @Test
    void shouldReturnTrueWhenStatusOk() {
        mockServer.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpProviderClient.API_KEY_HEADER, "secret-key"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(client.isPhoneAvailable(PROVIDER, PHONE)).isTrue();
        mockServer.verify();
    }

    @Test
    void shouldReturnFalseWhenNotFound() {
        mockServer.expect(requestTo(URL))
                .andRespond(withStatus(NOT_FOUND));

        assertThat(client.isPhoneAvailable(PROVIDER, PHONE)).isFalse();
        mockServer.verify();
    }

    @Test
    void shouldReturnFalseWhenStatusIsNotOk() {
        mockServer.expect(requestTo(URL))
                .andRespond(withSuccess("{\"status\":\"blocked\"}", MediaType.APPLICATION_JSON));

        assertThat(client.isPhoneAvailable(PROVIDER, PHONE)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenSuccessBodyIsMissing() {
        mockServer.expect(requestTo(URL))
                .andRespond(withSuccess());

        assertThat(client.isPhoneAvailable(PROVIDER, PHONE)).isFalse();
    }

    @Test
    void shouldNotDuplicateSlashWhenBaseUrlEndsWithSlash() {
        Provider providerWithTrailingSlash = new Provider("p1", "https://api.example.com/", "secret-key");

        mockServer.expect(requestTo(URL))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertThat(client.isPhoneAvailable(providerWithTrailingSlash, PHONE)).isTrue();
        mockServer.verify();
    }

    @Test
    void shouldPropagateOtherHttpErrors() {
        mockServer.expect(requestTo(URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.isPhoneAvailable(PROVIDER, PHONE))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void shouldPropagateNetworkError() {
        mockServer.expect(requestTo(URL))
                .andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> client.isPhoneAvailable(PROVIDER, PHONE))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("connection refused");
    }
}

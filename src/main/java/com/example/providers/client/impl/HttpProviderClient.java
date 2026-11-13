package com.example.providers.client.impl;

import com.example.providers.client.ProviderClient;
import com.example.providers.domain.Provider;
import com.example.providers.dto.StatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * GET {baseUrl}/subscribers/{phone}: 200 + {"status":"ok"} — номер доступен,
 * 404 — недоступен, остальные ошибки пробрасываются вызывающему коду.
 */
@Component
@RequiredArgsConstructor
public class HttpProviderClient implements ProviderClient {

    static final String API_KEY_HEADER = "X-API-Key";
    static final String STATUS_OK = "ok";

    private final RestClient restClient;

    @Override
    public boolean isPhoneAvailable(Provider provider, String phone) {
        try {
            // Телефон — через URI-шаблон, а не конкатенацией: иначе "+7" или пробел соберут битый URL.
            // Ключ в заголовке, не в query — URL попадает в логи, заголовки нет.
            StatusResponse body = restClient.get()
                    .uri(provider.baseUrl() + "/subscribers/{phone}", phone)
                    .header(API_KEY_HEADER, provider.apiKey())
                    .retrieve()
                    .body(StatusResponse.class);

            // Пустое тело или status != "ok" — значит недоступен.
            // Если же 200 пришёл вообще не с JSON (например, страница прокси) — это ошибка парсинга,
            // и она уйдёт наверх вместе с остальными. Молча превращать её в false нельзя.
            return body != null && STATUS_OK.equals(body.status());
        } catch (HttpClientErrorException.NotFound e) {
            // Наверх идёт всё, кроме 404: по ТЗ 404 — это просто «недоступен».
            return false;
        }
    }
}

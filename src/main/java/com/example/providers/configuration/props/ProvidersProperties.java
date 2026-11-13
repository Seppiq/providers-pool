package com.example.providers.configuration.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Настройки источника поставщиков.
 */
@Data
@ConfigurationProperties(prefix = "providers")
public class ProvidersProperties {

    /**
     * Путь к json-файлу с поставщиками.
     */
    private String jsonFile;
}

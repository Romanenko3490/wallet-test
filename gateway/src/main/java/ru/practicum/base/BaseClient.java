package ru.practicum.base;

import org.springframework.web.reactive.function.client.WebClient;

public class BaseClient {
    protected final WebClient webClient;

    public BaseClient(String baseUrl, String apiPrefix) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl + apiPrefix)
                .build();
    }
}

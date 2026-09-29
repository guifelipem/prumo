package com.prumo.transaction.integration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.MDC;

@Component
public class IdentityClient {
    private final RestClient client;
    private final String serviceKey;

    public IdentityClient(@Value("${app.auth-base-url}") String authBaseUrl,
                          @Value("${app.internal-service-key}") String serviceKey) {
        if (serviceKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("INTERNAL_SERVICE_KEY deve ter pelo menos 32 bytes.");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.client = RestClient.builder().baseUrl(authBaseUrl).requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String id = MDC.get("correlationId");
                    if (id != null) request.getHeaders().set("X-Correlation-Id", id);
                    return execution.execute(request, body);
                }).build();
        this.serviceKey = serviceKey;
    }

    public UUID requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            Identity identity = client.post().uri("/internal/sessions/introspect")
                    .header("X-Service-Key", serviceKey)
                    .header("Authorization", authorization)
                    .retrieve().body(Identity.class);
            if (identity == null || identity.userId() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            }
            return identity.userId();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticação indisponível", exception);
        }
    }

    record Identity(UUID userId) {}
}

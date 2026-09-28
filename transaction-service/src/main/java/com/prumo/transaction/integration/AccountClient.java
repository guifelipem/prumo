package com.prumo.transaction.integration;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccountClient {

    private final RestClient client;

    public AccountClient(@Value("${app.account-base-url}") String accountBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.client = RestClient.builder().baseUrl(accountBaseUrl).requestFactory(factory).build();
    }

    public UUID requireOwner(UUID accountId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        try {
            OwnedAccount account = client.get().uri("/accounts/{id}", accountId)
                    .header("Authorization", authorization)
                    .retrieve().body(OwnedAccount.class);
            if (account == null || account.ownerId() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            }
            return account.ownerId();
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Contas indisponíveis", exception);
        }
    }

    record OwnedAccount(UUID ownerId) {
    }
}

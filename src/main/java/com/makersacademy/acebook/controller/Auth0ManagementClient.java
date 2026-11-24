package com.makersacademy.acebook.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class Auth0ManagementClient {

    private final RestClient restClient = RestClient.create();
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;

    public Auth0ManagementClient(
            @Value("${okta.oauth2.issuer}") String issuer,
            @Value("${okta.oauth2.auth0.management.client-id}") String clientId,
            @Value("${okta.oauth2.auth0.management.client-secret}") String clientSecret) {
        this.baseUrl = issuer.endsWith("/") ? issuer : issuer + "/";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void deleteUser(String auth0UserId) {
        restClient.delete()
                .uri(baseUrl + "api/v2/users/{id}", auth0UserId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchToken())
                .retrieve()
                .toBodilessEntity();
    }

    private String fetchToken() {
        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "client_id", clientId,
                "client_secret", clientSecret,
                "audience", baseUrl + "api/v2/");

        Map<?, ?> response = restClient.post()
                .uri(baseUrl + "oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        return (String) response.get("access_token");
    }
}
package com.makersacademy.acebook.auth0;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class Auth0ManagementClient {

    private final RestClient restClient = RestClient.create();
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public Auth0ManagementClient(
            @Value("${okta.oauth2.issuer}") String issuer,
            @Value("${okta.oauth2.client-id}") String clientId,
            @Value("${okta.oauth2.client-secret}") String clientSecret) {
        this.baseUrl = issuer.endsWith("/") ? issuer : issuer + "/";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<Map<String, Object>> listUsers() {
        return restClient.get()
                .uri(baseUrl + "api/v2/users?per_page=50&fields=user_id,email,name,created_at&include_fields=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }

    public void deleteUser(String auth0UserId) {
        restClient.delete()
                .uri(baseUrl + "api/v2/users/{id}", auth0UserId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .toBodilessEntity();
    }

    private synchronized String token() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        Map<?, ?> response = restClient.post()
                .uri(baseUrl + "oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "grant_type", "client_credentials",
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "audience", baseUrl + "api/v2/"))
                .retrieve()
                .body(Map.class);

        cachedToken = (String) response.get("access_token");
        tokenExpiry = Instant.now().plusSeconds(((Number) response.get("expires_in")).longValue() - 60);
        return cachedToken;
    }
}
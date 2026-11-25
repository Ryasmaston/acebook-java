package com.makersacademy.acebook.auth0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class Auth0ManagementClientTest {

    private record Recorded(String method, String path, String query, String auth, String body) {}

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private volatile int apiStatus = 200;
    private volatile String apiBody = "{}";
    private volatile int expiresIn = 86400;

    private Auth0ManagementClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Recorded(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getQuery(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    body));

            boolean tokenCall = exchange.getRequestURI().getPath().equals("/oauth/token");
            int status = tokenCall ? 200 : apiStatus;
            String response = tokenCall
                    ? "{\"access_token\":\"test-token\",\"expires_in\":" + expiresIn + "}"
                    : apiBody;

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (bytes.length == 0) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();

        client = new Auth0ManagementClient(
                "http://localhost:" + server.getAddress().getPort() + "/",
                "my-client-id", "my-client-secret", "Username-Password-Authentication");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void requestsTokenWithClientCredentials() throws Exception {
        client.deleteUser("auth0|123");

        Recorded tokenRequest = requests.get(0);
        assertThat(tokenRequest.method()).isEqualTo("POST");
        assertThat(tokenRequest.path()).isEqualTo("/oauth/token");
        JsonNode body = JSON.readTree(tokenRequest.body());
        assertThat(body.get("grant_type").asText()).isEqualTo("client_credentials");
        assertThat(body.get("client_id").asText()).isEqualTo("my-client-id");
        assertThat(body.get("client_secret").asText()).isEqualTo("my-client-secret");
        assertThat(body.get("audience").asText()).endsWith("/api/v2/");
    }

    @Test
    void cachesTokenAcrossCalls() {
        client.deleteUser("auth0|1");
        client.deleteUser("auth0|2");

        assertThat(requests.stream().filter(r -> r.path().equals("/oauth/token"))).hasSize(1);
    }

    @Test
    void refetchesTokenWhenItHasExpired() {
        expiresIn = 30;   // the client refreshes 60s early, so this is already expired

        client.deleteUser("auth0|1");
        client.deleteUser("auth0|2");

        assertThat(requests.stream().filter(r -> r.path().equals("/oauth/token"))).hasSize(2);
    }

    @Test
    void deleteUserSendsDeleteWithBearerToken() {
        client.deleteUser("auth0|123");

        Recorded call = lastApiCall();
        assertThat(call.method()).isEqualTo("DELETE");
        assertThat(call.path()).isEqualTo("/api/v2/users/auth0|123");
        assertThat(call.auth()).isEqualTo("Bearer test-token");
    }

    @Test
    void listUsersReturnsParsedUsers() {
        apiBody = "[{\"user_id\":\"auth0|1\",\"email\":\"a@email.com\"},"
                + "{\"user_id\":\"auth0|2\",\"email\":\"b@email.com\"}]";

        List<Map<String, Object>> users = client.listUsers();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).get("email")).isEqualTo("a@email.com");
        Recorded call = lastApiCall();
        assertThat(call.method()).isEqualTo("GET");
        assertThat(call.path()).isEqualTo("/api/v2/users");
        assertThat(call.query()).contains("per_page=50");
        assertThat(call.auth()).isEqualTo("Bearer test-token");
    }

    @Test
    void createUserSendsEmailPasswordAndConnectionAndReturnsUser() throws Exception {
        apiBody = "{\"user_id\":\"auth0|new\",\"email\":\"a@email.com\"}";

        Map<String, Object> created = client.createUser("a@email.com", "StrongPass1!");

        assertThat(created.get("user_id")).isEqualTo("auth0|new");
        Recorded call = lastApiCall();
        assertThat(call.method()).isEqualTo("POST");
        assertThat(call.path()).isEqualTo("/api/v2/users");
        JsonNode body = JSON.readTree(call.body());
        assertThat(body.get("email").asText()).isEqualTo("a@email.com");
        assertThat(body.get("password").asText()).isEqualTo("StrongPass1!");
        assertThat(body.get("connection").asText()).isEqualTo("Username-Password-Authentication");
    }

    @Test
    void updateUserSendsPatchWithGivenChanges() throws Exception {
        client.updateUser("auth0|123", Map.of("nickname", "al"));

        Recorded call = lastApiCall();
        assertThat(call.method()).isEqualTo("PATCH");
        assertThat(call.path()).isEqualTo("/api/v2/users/auth0|123");
        assertThat(JSON.readTree(call.body()).get("nickname").asText()).isEqualTo("al");
    }

    @Test
    void updateEmailSendsEmailVerifyFlagAndConnection() throws Exception {
        client.updateEmail("auth0|123", "new@email.com");

        JsonNode body = JSON.readTree(lastApiCall().body());
        assertThat(body.get("email").asText()).isEqualTo("new@email.com");
        assertThat(body.get("verify_email").asBoolean()).isTrue();
        assertThat(body.get("connection").asText()).isEqualTo("Username-Password-Authentication");
        assertThat(body.has("password")).isFalse();
    }

    @Test
    void updatePasswordSendsOnlyPasswordAndConnection() throws Exception {
        client.updatePassword("auth0|123", "NewStrongPass1!");

        JsonNode body = JSON.readTree(lastApiCall().body());
        assertThat(body.get("password").asText()).isEqualTo("NewStrongPass1!");
        assertThat(body.get("connection").asText()).isEqualTo("Username-Password-Authentication");
        assertThat(body.has("email")).isFalse();
    }

    @Test
    void updateUsernameSendsNameAndConnection() throws Exception {
        client.updateUsername("auth0|123", "alice");

        JsonNode body = JSON.readTree(lastApiCall().body());
        assertThat(body.get("name").asText()).isEqualTo("alice");
        assertThat(body.get("connection").asText()).isEqualTo("Username-Password-Authentication");
        assertThat(body.has("email")).isFalse();
    }

    @Test
    void forbiddenResponseSurfacesAsException() {
        apiStatus = 403;
        apiBody = "{\"errorCode\":\"insufficient_scope\"}";

        assertThatThrownBy(() -> client.deleteUser("auth0|123"))
                .isInstanceOf(HttpClientErrorException.Forbidden.class)
                .hasMessageContaining("insufficient_scope");
    }

    private Recorded lastApiCall() {
        return requests.get(requests.size() - 1);
    }
}
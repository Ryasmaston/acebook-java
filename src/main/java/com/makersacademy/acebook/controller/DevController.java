package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.auth0.Auth0ManagementClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile("dev")
public class DevController {

    private final Auth0ManagementClient auth0Client;

    public DevController(Auth0ManagementClient auth0Client) {
        this.auth0Client = auth0Client;
    }

    @GetMapping("/dev/auth0-users")
    public List<Map<String, Object>> listAuth0Users() {
        return auth0Client.listUsers();
    }
}
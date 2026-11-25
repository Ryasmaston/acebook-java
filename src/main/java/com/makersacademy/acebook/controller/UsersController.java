package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.auth0.Auth0ManagementClient;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class UsersController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    Auth0ManagementClient auth0Client;

    @GetMapping("/users/after-login")
    public RedirectView afterLogin(@AuthenticationPrincipal OidcUser principal) {
        String email = principal.getEmail();
        userRepository.findUserByUsername(email)
                .orElseGet(() -> userRepository.save(new User(email)));
        return new RedirectView("/posts");
    }

    @DeleteMapping("/users/me")
    @Transactional
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal OidcUser principal,
                                                HttpServletRequest request) {
        userRepository.findUserByUsername(principal.getEmail())
                .ifPresent(userRepository::delete);        // local
        auth0Client.deleteUser(principal.getSubject());    // Auth0
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}

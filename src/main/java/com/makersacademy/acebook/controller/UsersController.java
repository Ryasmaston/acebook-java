package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.auth0.Auth0ManagementClient;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
public class UsersController {

    public record UserResponse(Long id, String username, boolean enabled, String email) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.isEnabled(), u.getEmail());
        }
    }

    public record UpdateUserRequest(String email, String password, String username) {}

    @Autowired
    UserRepository userRepository;
    @Autowired
    Auth0ManagementClient auth0Client;

    @GetMapping("/users/me") // get logged-in user
    public UserResponse findMe(@AuthenticationPrincipal OidcUser principal) {
        User user = userRepository.findUserByEmail(principal.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user);
    }

    @GetMapping("/users/after-login") // create user in local db after auth0 creation
    public RedirectView afterLogin(@AuthenticationPrincipal OidcUser principal) {
        String email = principal.getEmail();
        userRepository.findUserByEmail(email)
                .orElseGet(() -> userRepository.save(new User(email)));
        return new RedirectView("/posts");
    }

    @GetMapping("/users") // find all local users
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/users/{id}") // find user by local id
    public UserResponse findById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PatchMapping("/users/me") // update logged-in user's email and/or password (local DB, then Auth0)
    @Transactional
    public UserResponse updateMe(@AuthenticationPrincipal OidcUser principal,
                                 @RequestBody UpdateUserRequest req,
                                 HttpServletRequest request) {
        if (req.email() == null && req.password() == null && req.username == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to update");
        }
        if (!principal.getSubject().startsWith("auth0|")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email and password are managed by your login provider");
        }
        User user = userRepository.findUserByEmail(principal.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boolean changingEmail = req.email() != null && !req.email().equals(user.getEmail());
        if (changingEmail && userRepository.findUserByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        if (changingEmail) {
            user.setEmail(req.email());
            userRepository.saveAndFlush(user);
        }
        if (req.password() != null) {
            auth0Client.updatePassword(principal.getSubject(), req.password());
        }
        if (req.username() != null) {
            auth0Client.updateUsername(principal.getSubject(), req.username());
            user.setUsername(req.username());
            userRepository.saveAndFlush(user);
        }
        if (changingEmail) {
            auth0Client.updateEmail(principal.getSubject(), req.email());
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
        }
        return UserResponse.from(user);
    }

    @DeleteMapping("/users/me")
    @Transactional
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal OidcUser principal,
                                                HttpServletRequest request) {
        userRepository.findUserByEmail(principal.getEmail())
                .ifPresent(userRepository::delete);        // local
        auth0Client.deleteUser(principal.getSubject());    // Auth0
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}

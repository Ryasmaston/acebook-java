package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(UsersController.class)
@AutoConfigureMockMvc(addFilters = false)

public class UsersControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testAfterLoginRedirectsToPosts() throws Exception {
        User user = new User();
        user.setUsername("test@email.com");
        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(user));
        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
    }

    @Test
    public void testAfterLoginSavesNewUserWhenNotFound() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.empty());
        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testAfterLoginSavesUsernameFromEmailClaim() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.empty());
        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("test@email.com");
    }

    @Test
    public void testAfterLoginDoesNotSaveExistingUser() throws Exception {
        User user = new User();
        user.setUsername("test@email.com");
        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(user));
        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
        verify(userRepository, never()).save(any());
    }

    public void loginAs(String email) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
                .subject("test-subject")
                .claim("email", email)
                .build();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DefaultOidcUser principal = new DefaultOidcUser(authorities, idToken);
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, authorities, "okta"));
    }
}

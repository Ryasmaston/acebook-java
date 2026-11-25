package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.auth0.Auth0ManagementClient;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsersController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UsersControllerMvcTest {

    private static final String AUTH0_ID = "auth0|123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private Auth0ManagementClient auth0Client;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testAfterLoginRedirectsToPosts() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByEmail("test@email.com"))
                .thenReturn(Optional.of(new User("test@email.com")));

        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
    }

    @Test
    public void testAfterLoginSavesNewUserWithUsernameAndEmailFromClaim() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByEmail("test@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("test@email.com");
        assertThat(captor.getValue().getEmail()).isEqualTo("test@email.com");
    }

    @Test
    public void testAfterLoginDoesNotSaveExistingUser() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByEmail("test@email.com"))
                .thenReturn(Optional.of(new User("test@email.com")));

        mockMvc.perform(get("/users/after-login")).andExpect(status().is3xxRedirection());

        verify(userRepository, never()).save(any());
    }

    @Test
    public void testFindMeReturnsLoggedInUser() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByEmail("test@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "test@email.com")));

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("test@email.com"))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    public void testFindMeReturns404WhenNoLocalUser() throws Exception {
        loginAs("ghost@email.com");
        when(userRepository.findUserByEmail("ghost@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/me")).andExpect(status().isNotFound());
    }

    @Test
    public void testFindAllReturnsAllUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(
                savedUser(1L, "a@email.com"), savedUser(2L, "b@email.com")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("a@email.com"))
                .andExpect(jsonPath("$[1].email").value("b@email.com"));
    }

    @Test
    public void testFindByIdReturnsUser() throws Exception {
        when(userRepository.findById(5L)).thenReturn(Optional.of(savedUser(5L, "a@email.com")));

        mockMvc.perform(get("/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("a@email.com"));
    }

    @Test
    public void testFindByIdReturns404WhenMissing() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/99")).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateWithNothingReturns400() throws Exception {
        loginAs("me@email.com");

        mockMvc.perform(patch("/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auth0Client);
    }

    @Test
    public void testUpdateBySocialUserReturns400AndDoesNotTouchAuth0() throws Exception {
        loginAs("me@email.com", "google-oauth2|999");

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NewStrongPass1!\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auth0Client);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    public void testUpdateReturns404WhenNoLocalUser() throws Exception {
        loginAs("ghost@email.com");
        when(userRepository.findUserByEmail("ghost@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NewStrongPass1!\"}"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(auth0Client);
    }

    @Test
    public void testUpdateToExistingEmailReturns409AndDoesNotTouchAuth0() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "me@email.com")));
        when(userRepository.findUserByEmail("taken@email.com"))
                .thenReturn(Optional.of(savedUser(2L, "taken@email.com")));

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@email.com\"}"))
                .andExpect(status().isConflict());

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auth0Client);
    }

    @Test
    public void testUpdateEmailSavesLocallyThenUpdatesAuth0() throws Exception {
        User me = savedUser(1L, "me@email.com");
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com")).thenReturn(Optional.of(me));

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@email.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@email.com"));

        InOrder order = inOrder(userRepository, auth0Client);
        order.verify(userRepository).saveAndFlush(me);
        order.verify(auth0Client).updateEmail(AUTH0_ID, "new@email.com");
        verify(auth0Client, never()).updatePassword(any(), any());
        verify(auth0Client, never()).updateUsername(any(), any());
    }

    @Test
    public void testUpdateEmailInvalidatesSession() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "me@email.com")));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(patch("/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@email.com\"}"))
                .andExpect(status().isOk());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    public void testUpdateWithSameEmailChangesNothing() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "me@email.com")));

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"me@email.com\"}"))
                .andExpect(status().isOk());

        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auth0Client);
    }

    @Test
    public void testUpdatePasswordOnlyCallsAuth0AndKeepsSession() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "me@email.com")));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(patch("/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NewStrongPass1!\"}"))
                .andExpect(status().isOk());

        verify(auth0Client).updatePassword(AUTH0_ID, "NewStrongPass1!");
        verify(auth0Client, never()).updateEmail(any(), any());
        verify(userRepository, never()).saveAndFlush(any());
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    public void testUpdateUsernameUpdatesAuth0AndLocalAndKeepsSession() throws Exception {
        User me = savedUser(1L, "me@email.com");
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com")).thenReturn(Optional.of(me));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(patch("/users/me")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("me@email.com"));

        verify(auth0Client).updateUsername(AUTH0_ID, "alice");
        verify(userRepository).saveAndFlush(me);
        verify(auth0Client, never()).updateEmail(any(), any());
        assertThat(me.getUsername()).isEqualTo("alice");
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    public void testUpdateEmailAndPasswordChangesPasswordBeforeEmail() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "me@email.com")));

        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@email.com\",\"password\":\"NewStrongPass1!\"}"))
                .andExpect(status().isOk());

        InOrder order = inOrder(auth0Client);
        order.verify(auth0Client).updatePassword(AUTH0_ID, "NewStrongPass1!");
        order.verify(auth0Client).updateEmail(AUTH0_ID, "new@email.com");
    }

    @Test
    public void testUpdatePropagatesAuth0Failure() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com"))
                .thenReturn(Optional.of(savedUser(1L, "me@email.com")));
        doThrow(new RuntimeException("403 insufficient_scope"))
                .when(auth0Client).updatePassword(any(), any());

        assertThatThrownBy(() -> mockMvc.perform(patch("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"NewStrongPass1!\"}")))
                .hasRootCauseMessage("403 insufficient_scope");
    }

    @Test
    public void testDeleteRemovesLocalUserThenAuth0User() throws Exception {
        User me = savedUser(1L, "me@email.com");
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com")).thenReturn(Optional.of(me));

        mockMvc.perform(delete("/users/me")).andExpect(status().isNoContent());

        InOrder order = inOrder(userRepository, auth0Client);
        order.verify(userRepository).delete(me);
        order.verify(auth0Client).deleteUser(AUTH0_ID);
    }

    @Test
    public void testDeleteStillCallsAuth0WhenNoLocalUserExists() throws Exception {
        loginAs("ghost@email.com");
        when(userRepository.findUserByEmail("ghost@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/users/me")).andExpect(status().isNoContent());

        verify(userRepository, never()).delete(any());
        verify(auth0Client).deleteUser(AUTH0_ID);
    }

    @Test
    public void testDeleteInvalidatesSession() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com")).thenReturn(Optional.empty());
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(delete("/users/me").session(session)).andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    public void testDeleteFailsAndKeepsSessionWhenAuth0Fails() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("403 insufficient_scope"))
                .when(auth0Client).deleteUser(AUTH0_ID);
        MockHttpSession session = new MockHttpSession();

        assertThatThrownBy(() -> mockMvc.perform(delete("/users/me").session(session)))
                .hasRootCauseMessage("403 insufficient_scope");

        assertThat(session.isInvalid()).isFalse();
    }

    // -- some helper stuff --

    private User savedUser(Long id, String email) {
        User u = new User(email);
        u.setId(id);
        return u;
    }

    private void loginAs(String email) {
        loginAs(email, AUTH0_ID);
    }

    private void loginAs(String email, String subject) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
                .subject(subject)
                .claim("email", email)
                .build();
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DefaultOidcUser principal = new DefaultOidcUser(authorities, idToken);
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, authorities, "okta"));
    }
}
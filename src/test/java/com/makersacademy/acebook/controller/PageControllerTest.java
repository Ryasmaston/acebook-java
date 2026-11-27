package com.makersacademy.acebook.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(PageController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PageControllerTest {

    private static final String AUTH0_ID = "auth0|123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PostRepository postRepository;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void redirectRootToPosts() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
    }

    @Test
    public void testMyProfileShowsLoggedInUserAndPosts() throws Exception {
        User me = savedUser(1L, "me@email.com");
        loginAs("me@email.com");
        when(userRepository.findUserByEmail("me@email.com")).thenReturn(Optional.of(me));
        List<Post> myPosts = List.of(new Post("My content", "My title", me));
        when(postRepository.findByUserId(1L)).thenReturn(myPosts);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", me))
                .andExpect(model().attribute("posts", myPosts))
                .andExpect(model().attribute("isOwnProfile", true));
    }

    @Test
    public void testMyProfileReturns404WhenNoLocalUser() throws Exception {
        loginAs("ghost@email.com");
        when(userRepository.findUserByEmail("ghost@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile")).andExpect(status().isNotFound());
    }

    @Test
    public void testViewSomeoneElsesProfileHidesPrivateInfo() throws Exception {
        User other = savedUser(2L, "other@email.com");
        loginAs("me@email.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));

        mockMvc.perform(get("/profile/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", other))
                .andExpect(model().attribute("isOwnProfile", false));
    }

    @Test
    public void testViewMyOwnProfileByIdShowsPrivateInfo() throws Exception {
        User me = savedUser(1L, "me@email.com");
        loginAs("me@email.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        mockMvc.perform(get("/profile/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isOwnProfile", true));
    }

    @Test
    public void testViewProfileReturns404WhenIdMissing() throws Exception {
        loginAs("me@email.com");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile/99")).andExpect(status().isNotFound());
    }

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

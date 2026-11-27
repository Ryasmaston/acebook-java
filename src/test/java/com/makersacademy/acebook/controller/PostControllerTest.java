package com.makersacademy.acebook.controller;


import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostRepository postRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private CommentRepository commentsRepository;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testPostGetIndexReturnsIndexView() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/index"));
    }

    @Test
    public void testPostsAddsAllPostsToModel() throws Exception {
        Post post = new Post();
        post.setContent("New post content");
        when(postRepository.findAllByOrderByIdDesc()).thenReturn(List.of(post));
        mockMvc.perform(get("/posts"))
                .andExpect(model().attribute("posts", List.of(post)));
    }

    @Test
    public void testPostsAddsEmptyPostForTheForm() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(model().attributeExists("post"));
    }

    @Test
    public void testCreatePostSavesPostForLoggedInUsers() throws Exception {
        User user = new User();
        user.setUsername("test@email.com");
        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(user));
        mockMvc.perform(post("/posts").param("content", "Test post content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Test post content");
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    public void testCreatePostErrorsWhenUserNotFound() throws Exception {
        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.empty());
        mockMvc.perform(post("/posts")
                .param("content", "Test post content"))
                .andExpect(status().isOk())
                .andExpect(view().name("/error"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    public void testDeletePostRemovesPostForOwner() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("test@email.com");

        Post post = new Post();
        post.setId(10L);
        post.setUser(user);

        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(user));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        mockMvc.perform(post("/posts/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(postRepository).delete(post);
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

    @Test
    public void testEditPostEditsPostForOwner() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("test@email.com");

        Post post = new Post();
        post.setId(11L);
        post.setUser(user);

        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(user));
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));

        mockMvc.perform(post("/posts").param("content", "Test post content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        Mockito.reset(postRepository);

        mockMvc.perform(post("/edit/11").param("content", "Edited content")
                .param("post_id", "11"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
        ArgumentCaptor<Post> editCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(editCaptor.capture());

        assertThat(editCaptor.getValue().getContent()).isEqualTo("Edited content");
    }
}

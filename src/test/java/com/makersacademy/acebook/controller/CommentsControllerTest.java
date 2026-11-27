package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CommentsControllerTest {
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
    public void testCreateCommentSavesCommentForLoggedInUsers() throws Exception {
        User user = createUser(1L, "test@email.com");
        Post post = createPost(12L, user);

        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(user));
        when(postRepository.findPostById(12L)).thenReturn(post);

        String requestUri = "/comments/" + post.getId().toString();
        mockMvc.perform(post(requestUri)
                .param("content", "Test comment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentsRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Test comment");
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getPost()).isSameAs(post);
    }

    @Test
    public void testCreateCommentRendersErrorWhenUserNotFound() throws Exception {
        loginAs("unknown@email.com");
        when(userRepository.findUserByUsername("unknown@email.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/comments/12")
                        .param("content", "Test comment"))
                .andExpect(status().isOk())
                .andExpect(view().name("/error"))
                .andExpect(model().attribute("errorMessage", "Operation failed - try again later"));

        verify(commentsRepository, never()).save(any());
    }

    @Test
    public void testUpdateCommentUpdatesContentForOwner() throws Exception {
        User owner = createUser(1L, "test@email.com");
        Post post = createPost(12L, owner);
        Comment existingComment = createComment(5L, "Old content", owner, post);

        loginAs("test@email.com");
        when(userRepository.findUserByUsername("test@email.com")).thenReturn(Optional.of(owner));
        when(postRepository.findPostById(12L)).thenReturn(post);
        when(commentsRepository.findCommentById(5L)).thenReturn(existingComment);

        mockMvc.perform(post("/comment/12/5")
                        .param("content", "Updated comment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentsRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(5L);
        assertThat(captor.getValue().getContent()).isEqualTo("Updated comment");
        assertThat(captor.getValue().getUser()).isSameAs(owner);
        assertThat(captor.getValue().getPost()).isSameAs(post);
    }

    @Test
    public void testDeleteCommentDeletesCommentForOwner() throws Exception {
        User owner = createUser(1L, "owner@email.com");
        Post post = createPost(12L, owner);
        Comment comment = createComment(5L, "My comment", owner, post);

        loginAs("owner@email.com");
        when(userRepository.findUserByUsername("owner@email.com")).thenReturn(Optional.of(owner));
        when(commentsRepository.findCommentById(5L)).thenReturn(comment);

        mockMvc.perform(post("/comment/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(commentsRepository).delete(comment);
    }

    @Test
    public void testDeleteCommentRendersErrorForNonOwner() throws Exception {
        User owner = createUser(1L, "owner@email.com");
        User attacker = createUser(2L, "attacker@email.com");
        Post post = createPost(12L, owner);
        Comment comment = createComment(5L, "Owner comment", owner, post);

        loginAs("attacker@email.com");
        when(userRepository.findUserByUsername("attacker@email.com")).thenReturn(Optional.of(attacker));
        when(commentsRepository.findCommentById(5L)).thenReturn(comment);

        mockMvc.perform(post("/comment/5/delete"))
                .andExpect(status().isOk())
                .andExpect(view().name("/error"))
                .andExpect(model().attribute("errorMessage", "You can only edit your own comments"));

        verify(commentsRepository, never()).delete(any());
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

    private User createUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(email);
        return user;
    }

    private Post createPost(Long id, User user) {
        Post post = new Post();
        post.setId(id);
        post.setUser(user);
        return post;
    }

    private Comment createComment(Long id, String content, User user, Post post) {
        Comment comment = new Comment(content);
        comment.setId(id);
        comment.setUser(user);
        comment.setPost(post);
        return comment;
    }

}


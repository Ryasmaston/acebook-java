package com.makersacademy.acebook.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CommentTest {

    private final Comment comment = new Comment("Test Comment");

    @Test
    public void commentHasContent() {
        assertThat(comment.getContent(), containsString("Test Comment"));
    }

    @Test
    public void commentStartsWithNoUser() {
        assertThat(comment.getUser(), nullValue());
    }

    @Test
    public void commentStartsWithNoPost() {
        assertThat(comment.getPost(), nullValue());
    }

    @Test
    public void commentCanBeLinkedToUser() {
        User user = new User("test@email.com", true);
        comment.setUser(user);
        assertThat(comment.getUser(), equalTo(user));
    }

    @Test
    public void commentCanBeLinkedToPost() {
        Post post = new Post("hello", "Test title", null);
        comment.setPost(post);
        assertThat(comment.getPost(), equalTo(post));
    }

    @Test
    public void commentContentCanBeChanged() {
        comment.setContent("Edited comment");
        assertThat(comment.getContent(), equalTo("Edited comment"));
    }
}
package com.makersacademy.acebook.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class UserTest {

    private User user = new User("username", true);

    @Test
    public void userHasUsername() {
        assertThat(user.getUsername(), containsString("username"));
    }

    @Test
    public void userIsEnabled() {
        assertThat(user.isEnabled(), equalTo(true));
    }

    @Test
    public void userCanBeDisabled() {
        assertThat(new User("username", false).isEnabled(), equalTo(false));
    }

    @Test
    public void singleArgConstructorSetsUsernameAndEmailAndEnables() {
        User u = new User("a@email.com");
        assertThat(u.getUsername(), equalTo("a@email.com"));
        assertThat(u.getEmail(), equalTo("a@email.com"));
        assertThat(u.isEnabled(), equalTo(true));
    }

    @Test
    public void twoArgConstructorLeavesEmailUnset() {
        assertThat(user.getEmail(), equalTo(null));
    }

    @Test
    public void defaultConstructorEnablesUserWithNoUsernameOrEmail() {
        User u = new User();
        assertThat(u.isEnabled(), equalTo(true));
        assertThat(u.getUsername(), equalTo(null));
        assertThat(u.getEmail(), equalTo(null));
    }

    @Test
    public void usernameAndEmailCanBeChangedIndependently() {
        User u = new User("a@email.com");
        u.setUsername("alice");
        u.setEmail("new@email.com");
        assertThat(u.getUsername(), equalTo("alice"));
        assertThat(u.getEmail(), equalTo("new@email.com"));
    }

    @Test
    public void userStartsWithNoPosts() {
        assertThat(user.getPosts(), notNullValue());
        assertThat(user.getPosts(), empty());
    }

    @Test
    public void toStringAndHashCodeDoNotLoopThroughPosts() {
        Post post = new Post("hello", "Test title", null);
        post.setUser(user);
        user.getPosts().add(post);

        assertDoesNotThrow(user::toString);
        assertDoesNotThrow(user::hashCode);
        assertDoesNotThrow(post::toString);
    }
}
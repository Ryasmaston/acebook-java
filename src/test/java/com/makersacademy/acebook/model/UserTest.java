package com.makersacademy.acebook.model;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.jupiter.api.Test;

public class UserTest {

    private User user = new User("username", true);

    @Test
    public void userHasUsername() {
        assertThat(user.getUsername(), containsString("username"));
    }

    @Test
    public void userIsEnabled(){ assertThat(user.isEnabled(), equalTo(true)); }
}

package com.makersacademy.acebook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

import java.util.ArrayList;

import static java.lang.Boolean.TRUE;

@Data
@Entity
@Table(name = "USERS")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private boolean enabled;
    private String email;
    private String bio;
    private String profilePictureUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Post> posts = new ArrayList<>();

    public User() {
        this.enabled = TRUE;
    }

    public User(String username) {
        this.username = username;
        this.enabled = TRUE;
        this.email = username;
    }

    public User(String username, boolean enabled) {
        this.username = username;
        this.enabled = enabled;
    }
}

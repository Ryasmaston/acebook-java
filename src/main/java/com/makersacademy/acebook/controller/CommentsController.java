package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class CommentsController {

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    PostRepository postRepository;

    @Autowired
    UserRepository userRepository;

    @PostMapping("/comments/{post_id}")
    public RedirectView create(@ModelAttribute Comment comment, @PathVariable Long post_id) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String userEmail = (String) principal.getAttributes().get("email");

        User loggedInUser = userRepository
                .findUserByUsername(userEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Operation failed - try again later"
                ));
        Post currentPost = postRepository
                .findPostById(post_id);

        comment.setUser(loggedInUser);
        comment.setPost(currentPost);

        commentRepository.save(comment);
        return new RedirectView("/posts");
    }

}

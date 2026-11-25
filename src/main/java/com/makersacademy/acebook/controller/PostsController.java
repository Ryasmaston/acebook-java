package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import net.minidev.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;


@Controller
public class PostsController {

    @Autowired
    PostRepository postRepository;

    @Autowired
    UserRepository userRepository;

    @GetMapping("/posts")
    public String index(Model model) {
        Iterable<Post> posts = postRepository.findAllByOrderByIdDesc();
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        model.addAttribute("posts", posts);
        model.addAttribute("post", new Post());
        return "posts/index";
    }

    @PostMapping("/posts")
    public RedirectView create(@ModelAttribute Post post) {
        /*CHALLENGE: Do you notice anything about this bit of code? Is it already in use someplace else?
        Could you maybe think of a way to make retrieving principal information easier throughout your app?*/
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String userEmail = (String) principal.getAttributes().get("email");

        /*CHALLENGE: Try and understand what happens if an error occurs here.
        * What template gets brought up?
        * What mechanism tells Springboot to load that template?
        * How can you use it to your advantage on your own routes to handle errors in a user-friendly-way?*/
            User loggedInUser = userRepository
                    .findUserByUsername(userEmail)
                    .orElseThrow(() -> new IllegalStateException(
                            "Operation failed - try again later"
                    ));
            post.setUser(loggedInUser);

            postRepository.save(post);
            return new RedirectView("/posts");
    }

    @GetMapping("/edit/{post_id}")
    public String showEdit(Model model, @PathVariable Long post_id) {
        Post post = postRepository.findPostById(post_id);

        model.addAttribute("post", post);
        return "posts/edit";
    }

    @PostMapping("/edit/{post_id}")
    public RedirectView update(@ModelAttribute Post post, @PathVariable Long post_id) {
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
        post.setUser(loggedInUser);
        post.setId(post_id);

        if (loggedInUser.getId().equals(post.getUser().getId())) {
            postRepository.save(post);
        } else {
            throw new IllegalStateException("You can only edit your own posts");
        }

        postRepository.save(post);
        return new RedirectView("/posts");
    }
}

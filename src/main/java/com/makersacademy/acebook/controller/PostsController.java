package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import net.minidev.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;


@Controller
public class PostsController {

    public record PostResponse(Long id, String content, Long user_id, String title) {
        static PostResponse from(Post post) {
            return new PostResponse(
                    post.getId(),
                    post.getContent(),
                    post.getUser().getId(),
                    post.getTitle()
            );
        }
    }

    @Autowired
    PostRepository postRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CommentRepository commentRepository;

    @GetMapping("/posts")
    public String index(Model model) {
        Iterable<Post> posts = postRepository.findAllByOrderByIdDesc();
        Iterable<Comment> comments = commentRepository.findAll();
        model.addAttribute("comments", comments);
        model.addAttribute("comment", new Comment());
        model.addAttribute("posts", posts);
        model.addAttribute("post", new Post());
        return "posts/index";
    }

    @GetMapping("/users/{userId}/posts") // find post by user id
    @ResponseBody
    public List<PostResponse> findByUserId(@PathVariable Long userId) {
        return postRepository.findByUserId(userId)
                .stream()
                .map(PostResponse::from)
                .toList();
    }

    @PostMapping("/posts")
    public RedirectView create(@ModelAttribute Post post) {
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

        return new RedirectView("/posts");
    }
  
    @PostMapping("/posts/{id}/delete")
    public RedirectView delete(@PathVariable Long id) {
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

        Post targetPost = postRepository
                .findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "No post found"
                ));

        User owner = targetPost.getUser();

        if (owner == null) {
            throw new IllegalStateException("You can only delete your own posts");
        } else if (owner.getId().equals(loggedInUser.getId())) {
            postRepository.delete(targetPost);
            return new RedirectView("/posts");
        } else {
            throw new IllegalStateException("You can only delete your own posts");
        }
    }
 
  
}

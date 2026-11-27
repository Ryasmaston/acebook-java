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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/comment/{comment_id}/edit")
    public String showEdit(Model model, @PathVariable Long comment_id) {
        Comment comment = commentRepository.findCommentById(comment_id);

        model.addAttribute("comment", comment);
        return "posts/edit-comment";
    }

    @PostMapping("comment/{post_id}/{id}")
    public RedirectView update(@ModelAttribute Comment newComment, @PathVariable Long post_id, @PathVariable Long id) {
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

        Comment existingComment = commentRepository.findCommentById(id);

        if (loggedInUser.getId().equals(existingComment.getUser().getId())) {
            existingComment.setContent(newComment.getContent());
            commentRepository.save(existingComment);
        } else {
            throw new IllegalStateException("You can only edit your own comments");
        }

        return new RedirectView("/posts");
    }

    @PostMapping("/comment/{id}/delete")
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

        Comment targetComment = commentRepository
                .findCommentById(id);

        if (loggedInUser.getId().equals(targetComment.getUser().getId())) {
            commentRepository.delete(targetComment);
        } else {
            throw new IllegalStateException("You can only edit your own comments");
        }
        return new RedirectView("/posts");
    }

}

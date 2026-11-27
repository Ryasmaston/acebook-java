package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
public class PageController {

	@Autowired
	UserRepository userRepository;

	@Autowired
	PostRepository postRepository;

	@GetMapping(value = "/")
	public RedirectView index() {
		return new RedirectView("/posts");
	}

	@GetMapping("/profile") // my own profile
	public String myProfile(@AuthenticationPrincipal OidcUser principal, Model model) {
		User user = userRepository.findUserByEmail(principal.getEmail())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		List<Post> posts = postRepository.findByUserId(user.getId());

		model.addAttribute("user", user);
		model.addAttribute("posts", posts);
		model.addAttribute("isOwnProfile", true);
		return "profile";
	}

	@GetMapping("/profile/{id}") // anyone's profile
	public String viewProfile(@PathVariable Long id, @AuthenticationPrincipal OidcUser principal, Model model) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		List<Post> posts = postRepository.findByUserId(user.getId());
		boolean isOwnProfile = principal.getEmail().equals(user.getEmail());

		model.addAttribute("user", user);
		model.addAttribute("posts", posts);
		model.addAttribute("isOwnProfile", isOwnProfile);
		return "profile";
	}
}

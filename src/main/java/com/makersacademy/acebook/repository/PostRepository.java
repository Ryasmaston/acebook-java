package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.model.Post;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PostRepository extends CrudRepository<Post, Long> {
    Post findPostById(Long id);
    List<Post> findByUserId(Long userId);
    Iterable<Post> findAllByOrderByIdDesc();
}

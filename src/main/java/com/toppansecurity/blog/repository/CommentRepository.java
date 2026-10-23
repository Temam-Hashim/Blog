package com.toppansecurity.blog.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toppansecurity.blog.model.BlogComment;

public interface CommentRepository extends JpaRepository<BlogComment, Long> {
	Page<BlogComment> findAllByPostId(Long postId, Pageable pageable);
}

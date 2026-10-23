package com.toppansecurity.blog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toppansecurity.blog.model.BlogPost;
import com.toppansecurity.blog.model.PostStatus;

public interface PostRepository extends JpaRepository<BlogPost, Long> {
	Page<BlogPost> findAllByStatus(PostStatus status, Pageable pageable);

	Page<BlogPost> findAllByAuthorId(Long authorId, Pageable pageable);

	Page<BlogPost> findAllByAuthorIdAndStatus(Long authorId, PostStatus status, Pageable pageable);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	java.util.Optional<BlogPost> findBySlugAndStatus(String slug, PostStatus status);
}

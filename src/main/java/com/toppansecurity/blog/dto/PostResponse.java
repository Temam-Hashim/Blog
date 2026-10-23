package com.toppansecurity.blog.dto;

import java.time.Instant;

import com.toppansecurity.blog.model.BlogPost;
import com.toppansecurity.blog.model.PostStatus;

public record PostResponse(Long id, String title, String slug, String excerpt, String content, PostStatus status,
		Long authorId, String authorName, Instant createdAt, Instant updatedAt) {
	public static PostResponse from(BlogPost post) {
		return new PostResponse(post.getId(), post.getTitle(), post.getSlug(), post.getExcerpt(), post.getContent(),
				post.getStatus(), post.getAuthor().getId(), post.getAuthor().getDisplayName(), post.getCreatedAt(),
				post.getUpdatedAt());
	}
}

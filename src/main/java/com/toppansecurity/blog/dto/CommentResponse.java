package com.toppansecurity.blog.dto;

import java.time.Instant;

import com.toppansecurity.blog.model.BlogComment;

public record CommentResponse(Long id, String content, Long authorId, String authorName, Instant createdAt) {
	public static CommentResponse from(BlogComment comment) {
		return new CommentResponse(comment.getId(), comment.getContent(), comment.getAuthor().getId(),
				comment.getAuthor().getDisplayName(), comment.getCreatedAt());
	}
}

package com.toppansecurity.blog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.toppansecurity.blog.dto.CommentRequest;
import com.toppansecurity.blog.dto.CommentResponse;
import com.toppansecurity.blog.model.BlogComment;
import com.toppansecurity.blog.repository.CommentRepository;
import com.toppansecurity.blog.repository.UserRepository;

@Service
public class CommentService {
	private final CommentRepository comments;
	private final UserRepository users;
	private final PostService posts;

	public CommentService(CommentRepository comments, UserRepository users, PostService posts) {
		this.comments = comments;
		this.users = users;
		this.posts = posts;
	}

	@Transactional(readOnly = true)
	public Page<CommentResponse> listForPublishedPost(String slug, int page, int size, Sort.Direction direction) {
		var post = posts.findPublishedEntity(slug);
		var pageable = PageRequestFactory.create(page, size, "createdAt", direction);
		return comments.findAllByPostId(post.getId(), pageable).map(CommentResponse::from);
	}

	@Transactional
	public CommentResponse create(String email, String slug, CommentRequest request) {
		var post = posts.findPublishedEntity(slug);
		var author = users.findByEmail(UserService.normalizeEmail(email))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		return CommentResponse.from(comments.save(new BlogComment(request.content().trim(), post, author)));
	}

	@Transactional
	public void delete(String email, Long commentId, boolean administrator) {
		var comment = comments.findById(commentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
		if (!administrator && !comment.getAuthor().getEmail().equals(UserService.normalizeEmail(email))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only delete your own comments");
		}
		comments.delete(comment);
	}
}

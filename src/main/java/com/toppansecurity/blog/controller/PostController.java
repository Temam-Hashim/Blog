package com.toppansecurity.blog.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.toppansecurity.blog.dto.CommentRequest;
import com.toppansecurity.blog.dto.CommentResponse;
import com.toppansecurity.blog.dto.PostRequest;
import com.toppansecurity.blog.dto.PostResponse;
import com.toppansecurity.blog.model.PostSort;
import com.toppansecurity.blog.model.PostStatus;
import com.toppansecurity.blog.service.CommentService;
import com.toppansecurity.blog.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class PostController {
	private final PostService posts;
	private final CommentService comments;

	public PostController(PostService posts, CommentService comments) {
		this.posts = posts;
		this.comments = comments;
	}

	@GetMapping("/posts")
	@Operation(summary = "List published posts")
	public Page<PostResponse> listPublished(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "CREATED_AT") PostSort sort,
			@RequestParam(defaultValue = "DESC") Sort.Direction direction) {
		return posts.listPublished(sort, direction, page, size);
	}

	@GetMapping("/posts/{slug}")
	@Operation(summary = "Read a published post")
	public PostResponse getPublished(@PathVariable String slug) {
		return posts.getPublished(slug);
	}

	@GetMapping("/posts/{slug}/comments")
	@Operation(summary = "List comments on a published post")
	public Page<CommentResponse> listComments(
			@PathVariable String slug,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "ASC") Sort.Direction direction) {
		return comments.listForPublishedPost(slug, page, size, direction);
	}

	@PostMapping("/posts/{slug}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Comment on a published post")
	public CommentResponse addComment(Authentication authentication, @PathVariable String slug,
			@Valid @RequestBody CommentRequest request) {
		return comments.create(authentication.getName(), slug, request);
	}

	@PostMapping("/posts")
	@ResponseStatus(HttpStatus.CREATED)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Create a post")
	public PostResponse create(Authentication authentication, @Valid @RequestBody PostRequest request) {
		return posts.create(authentication.getName(), request);
	}

	@GetMapping("/me/posts")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "List your posts")
	public Page<PostResponse> listMine(Authentication authentication,
			@RequestParam(required = false) PostStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "CREATED_AT") PostSort sort,
			@RequestParam(defaultValue = "DESC") Sort.Direction direction) {
		return posts.listMine(authentication.getName(), status, sort, direction, page, size);
	}

	@PutMapping("/me/posts/{id}")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Update one of your posts")
	public PostResponse update(Authentication authentication, @PathVariable Long id,
			@Valid @RequestBody PostRequest request) {
		return posts.update(authentication.getName(), id, request, isAdmin(authentication));
	}

	@DeleteMapping("/me/posts/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Delete one of your posts")
	public void delete(Authentication authentication, @PathVariable Long id) {
		posts.delete(authentication.getName(), id, isAdmin(authentication));
	}

	@DeleteMapping("/comments/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Delete your comment")
	public void deleteComment(Authentication authentication, @PathVariable Long id) {
		comments.delete(authentication.getName(), id, isAdmin(authentication));
	}

	private static boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}

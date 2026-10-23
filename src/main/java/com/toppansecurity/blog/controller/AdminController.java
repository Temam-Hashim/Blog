package com.toppansecurity.blog.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.toppansecurity.blog.dto.PostResponse;
import com.toppansecurity.blog.dto.UserResponse;
import com.toppansecurity.blog.model.PostStatus;
import com.toppansecurity.blog.model.PostSort;
import com.toppansecurity.blog.model.Role;
import com.toppansecurity.blog.model.UserSort;
import com.toppansecurity.blog.service.PostService;
import com.toppansecurity.blog.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
	private final UserService users;
	private final PostService posts;

	public AdminController(UserService users, PostService posts) {
		this.users = users;
		this.posts = posts;
	}

	@GetMapping("/users")
	@Operation(summary = "List all users (admin only)")
	public Page<UserResponse> listUsers(
			@RequestParam(required = false) Role role,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "CREATED_AT") UserSort sort,
			@RequestParam(defaultValue = "DESC") Sort.Direction direction) {
		return users.listUsers(role, sort, direction, page, size);
	}

	@GetMapping("/posts")
	@Operation(summary = "List all posts, optionally filtered by status (admin only)")
	public Page<PostResponse> listPosts(
			@RequestParam(required = false) PostStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "CREATED_AT") PostSort sort,
			@RequestParam(defaultValue = "DESC") Sort.Direction direction) {
		return posts.listForAdmin(status, sort, direction, page, size);
	}

	@PatchMapping("/posts/{id}/status")
	@Operation(summary = "Publish or unpublish a post (admin only)")
	public PostResponse moderate(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
		return posts.moderate(id, request.status());
	}

	public record StatusRequest(@jakarta.validation.constraints.NotNull PostStatus status) {
	}
}

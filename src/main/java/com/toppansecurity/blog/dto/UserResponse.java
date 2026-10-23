package com.toppansecurity.blog.dto;

import java.time.Instant;

import com.toppansecurity.blog.model.BlogUser;
import com.toppansecurity.blog.model.Role;

public record UserResponse(Long id, String email, String displayName, Role role, Instant createdAt) {
	public static UserResponse from(BlogUser user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
				user.getCreatedAt());
	}
}

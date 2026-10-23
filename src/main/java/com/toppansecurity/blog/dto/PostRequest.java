package com.toppansecurity.blog.dto;

import com.toppansecurity.blog.model.PostStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
		@NotBlank @Size(max = 180) String title,
		@Size(max = 200) String slug,
		@Size(max = 500) String excerpt,
		@NotBlank String content,
		PostStatus status) {
}

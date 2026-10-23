package com.toppansecurity.blog.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldViolation> fieldErrors) {
	public record FieldViolation(String field, String message) {
	}
}

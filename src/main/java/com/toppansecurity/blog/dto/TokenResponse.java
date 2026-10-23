package com.toppansecurity.blog.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}

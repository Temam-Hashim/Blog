package com.toppansecurity.blog.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toppansecurity.blog.dto.LoginRequest;
import com.toppansecurity.blog.dto.RegisterRequest;
import com.toppansecurity.blog.dto.TokenResponse;
import com.toppansecurity.blog.dto.UserResponse;
import com.toppansecurity.blog.service.AuthService;
import com.toppansecurity.blog.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final UserService users;
	private final AuthService auth;

	public AuthController(UserService users, AuthService auth) {
		this.users = users;
		this.auth = auth;
	}

	@PostMapping("/register")
	@org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a user account (always assigned the USER role)")
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return users.register(request);
	}

	@PostMapping("/login")
	@Operation(summary = "Authenticate and receive a bearer access token")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return auth.login(request);
	}
}

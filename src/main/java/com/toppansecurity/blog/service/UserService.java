package com.toppansecurity.blog.service;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.toppansecurity.blog.dto.RegisterRequest;
import com.toppansecurity.blog.dto.UserResponse;
import com.toppansecurity.blog.model.BlogUser;
import com.toppansecurity.blog.model.Role;
import com.toppansecurity.blog.model.UserSort;
import com.toppansecurity.blog.repository.UserRepository;

@Service
public class UserService {
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		var email = normalizeEmail(request.email());
		if (users.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
		}
		var user = users.save(new BlogUser(email, request.displayName().trim(),
				passwordEncoder.encode(request.password()), Role.USER));
		return UserResponse.from(user);
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> listUsers(Role role, UserSort sort, Sort.Direction direction, int page, int size) {
		var pageable = PageRequestFactory.create(page, size, sort.property(), direction);
		return (role == null ? users.findAll(pageable) : users.findAllByRole(role, pageable))
				.map(UserResponse::from);
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}

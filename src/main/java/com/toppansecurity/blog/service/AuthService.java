package com.toppansecurity.blog.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.toppansecurity.blog.dto.LoginRequest;
import com.toppansecurity.blog.dto.TokenResponse;
import com.toppansecurity.blog.repository.UserRepository;

@Service
public class AuthService {
	private static final long TOKEN_LIFETIME_SECONDS = 900;

	private final AuthenticationManager authenticationManager;
	private final UserRepository users;
	private final JwtEncoder jwtEncoder;

	public AuthService(AuthenticationManager authenticationManager, UserRepository users, JwtEncoder jwtEncoder) {
		this.authenticationManager = authenticationManager;
		this.users = users;
		this.jwtEncoder = jwtEncoder;
	}

	public TokenResponse login(LoginRequest request) {
		var email = UserService.normalizeEmail(request.email());
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.password()));
		} catch (AuthenticationException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}

		var user = users.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
		var issuedAt = Instant.now();
		var expiresAt = issuedAt.plus(TOKEN_LIFETIME_SECONDS, ChronoUnit.SECONDS);
		var claims = JwtClaimsSet.builder()
				.issuer("blog-api")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getEmail())
				.claim("roles", java.util.List.of(user.getRole().name()))
				.build();
		var header = JwsHeader.with(MacAlgorithm.HS256).build();
		var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", TOKEN_LIFETIME_SECONDS);
	}
}

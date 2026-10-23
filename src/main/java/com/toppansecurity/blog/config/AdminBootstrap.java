package com.toppansecurity.blog.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.toppansecurity.blog.model.BlogUser;
import com.toppansecurity.blog.model.Role;
import com.toppansecurity.blog.repository.UserRepository;
import com.toppansecurity.blog.service.UserService;

@Configuration
public class AdminBootstrap {
	@Bean
	ApplicationRunner createBootstrapAdmin(UserRepository users, PasswordEncoder passwordEncoder,
			org.springframework.core.env.Environment environment) {
		return args -> {
			var localProperties = loadLocalProperties();
			var email = localProperties.getProperty("BLOG_ADMIN_EMAIL",
					environment.getProperty("BLOG_ADMIN_EMAIL", "")).trim();
			var password = localProperties.getProperty("BLOG_ADMIN_PASSWORD",
					environment.getProperty("BLOG_ADMIN_PASSWORD", ""));
			if (email.isEmpty() && password.isEmpty()) {
				return;
			}
			if (email.isEmpty() || password.isBlank()) {
				throw new IllegalStateException("Set both BLOG_ADMIN_EMAIL and BLOG_ADMIN_PASSWORD to bootstrap an admin");
			}
			if (password.length() < 12 || password.length() > 72) {
				throw new IllegalStateException("BLOG_ADMIN_PASSWORD must be between 12 and 72 characters");
			}
			var normalizedEmail = UserService.normalizeEmail(email);
			var existing = users.findByEmail(normalizedEmail);
			if (existing.isPresent()) {
				if (existing.get().getRole() != Role.ADMIN) {
					throw new IllegalStateException("The configured bootstrap email belongs to a non-admin user");
				}
				return;
			}
			users.save(new BlogUser(normalizedEmail, "Administrator", passwordEncoder.encode(password), Role.ADMIN));
		};
	}

	private Properties loadLocalProperties() {
		var properties = new Properties();
		var envFile = Path.of(".env");
		if (!Files.isRegularFile(envFile)) {
			return properties;
		}
		try (Reader reader = Files.newBufferedReader(envFile)) {
			properties.load(reader);
			return properties;
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to read the project .env file", exception);
		}
	}
}

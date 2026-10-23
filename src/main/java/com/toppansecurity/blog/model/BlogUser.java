package com.toppansecurity.blog.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "blog_users", uniqueConstraints = @UniqueConstraint(name = "uk_blog_users_email", columnNames = "email"))
public class BlogUser {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false, length = 100)
	private String displayName;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected BlogUser() {
	}

	public BlogUser(String email, String displayName, String passwordHash, Role role) {
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

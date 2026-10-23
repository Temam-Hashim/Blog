package com.toppansecurity.blog.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "blog_posts", uniqueConstraints = @UniqueConstraint(name = "uk_blog_posts_slug", columnNames = "slug"))
public class BlogPost {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(nullable = false, length = 200)
	private String slug;

	@Column(length = 500)
	private String excerpt;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PostStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private BlogUser author;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected BlogPost() {
	}

	public BlogPost(String title, String slug, String excerpt, String content, PostStatus status, BlogUser author) {
		this.title = title;
		this.slug = slug;
		this.excerpt = excerpt;
		this.content = content;
		this.status = status;
		this.author = author;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void update(String title, String slug, String excerpt, String content, PostStatus status) {
		this.title = title;
		this.slug = slug;
		this.excerpt = excerpt;
		this.content = content;
		this.status = status;
	}

	public void setStatus(PostStatus status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getSlug() {
		return slug;
	}

	public String getExcerpt() {
		return excerpt;
	}

	public String getContent() {
		return content;
	}

	public PostStatus getStatus() {
		return status;
	}

	public BlogUser getAuthor() {
		return author;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}

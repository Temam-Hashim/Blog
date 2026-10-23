package com.toppansecurity.blog.model;

import java.time.Instant;

import org.springframework.validation.annotation.Validated;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Entity
@Table(name = "blog_comments") 
@Getter 
public class BlogComment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = "text")
	@NotBlank(message = "Content is required")
	@Size(min = 2, max = 500, message = "comment must be between 2 and 500 characters")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "post_id", nullable = false)
	private BlogPost post;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private BlogUser author;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected BlogComment() {
	}

	public BlogComment(String content, BlogPost post, BlogUser author) {
		this.content = content;
		this.post = post;
		this.author = author;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

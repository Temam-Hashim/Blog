package com.toppansecurity.blog.service;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.toppansecurity.blog.dto.PostRequest;
import com.toppansecurity.blog.dto.PostResponse;
import com.toppansecurity.blog.model.BlogPost;
import com.toppansecurity.blog.model.BlogUser;
import com.toppansecurity.blog.model.PostStatus;
import com.toppansecurity.blog.model.PostSort;
import com.toppansecurity.blog.repository.PostRepository;
import com.toppansecurity.blog.repository.UserRepository;

@Service
public class PostService {
	private final PostRepository posts;
	private final UserRepository users;

	public PostService(PostRepository posts, UserRepository users) {
		this.posts = posts;
		this.users = users;
	}

	@Transactional(readOnly = true)
	public Page<PostResponse> listPublished(PostSort sort, Sort.Direction direction, int page, int size) {
		var pageable = PageRequestFactory.create(page, size, sort.property(), direction);
		return posts.findAllByStatus(PostStatus.PUBLISHED, pageable).map(PostResponse::from);
	}

	@Transactional(readOnly = true)
	public PostResponse getPublished(String slug) {
		return PostResponse.from(posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found")));
	}

	@Transactional(readOnly = true)
	public Page<PostResponse> listMine(String email, PostStatus status, PostSort sort, Sort.Direction direction,
			int page, int size) {
		var user = findUser(email);
		var pageable = PageRequestFactory.create(page, size, sort.property(), direction);
		var results = status == null
				? posts.findAllByAuthorId(user.getId(), pageable)
				: posts.findAllByAuthorIdAndStatus(user.getId(), status, pageable);
		return results.map(PostResponse::from);
	}

	@Transactional
	public PostResponse create(String email, PostRequest request) {
		var author = findUser(email);
		var slug = uniqueSlug(request.slug(), request.title(), null);
		var post = new BlogPost(request.title().trim(), slug, normalizeOptional(request.excerpt()),
				request.content().trim(), request.status() == null ? PostStatus.DRAFT : request.status(), author);
		return PostResponse.from(posts.save(post));
	}

	@Transactional
	public PostResponse update(String email, Long postId, PostRequest request, boolean administrator) {
		var post = findPost(postId);
		if (!administrator && !post.getAuthor().getEmail().equals(UserService.normalizeEmail(email))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only edit your own posts");
		}
		var slug = uniqueSlug(request.slug(), request.title(), postId);
		post.update(request.title().trim(), slug, normalizeOptional(request.excerpt()), request.content().trim(),
				request.status() == null ? post.getStatus() : request.status());
		return PostResponse.from(post);
	}

	@Transactional
	public void delete(String email, Long postId, boolean administrator) {
		var post = findPost(postId);
		if (!administrator && !post.getAuthor().getEmail().equals(UserService.normalizeEmail(email))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only delete your own posts");
		}
		posts.delete(post);
	}

	@Transactional(readOnly = true)
	public Page<PostResponse> listForAdmin(PostStatus status, PostSort sort, Sort.Direction direction, int page,
			int size) {
		var pageable = PageRequestFactory.create(page, size, sort.property(), direction);
		return (status == null ? posts.findAll(pageable) : posts.findAllByStatus(status, pageable))
				.map(PostResponse::from);
	}

	@Transactional
	public PostResponse moderate(Long postId, PostStatus status) {
		var post = findPost(postId);
		post.setStatus(status);
		return PostResponse.from(post);
	}

	@Transactional(readOnly = true)
	public BlogPost findPublishedEntity(String slug) {
		return posts.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
	}

	@Transactional(readOnly = true)
	public BlogPost findPost(Long id) {
		return posts.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
	}

	private BlogUser findUser(String email) {
		return users.findByEmail(UserService.normalizeEmail(email))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private String uniqueSlug(String requestedSlug, String title, Long currentPostId) {
		var base = slugify(requestedSlug == null || requestedSlug.isBlank() ? title : requestedSlug);
		if (base.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid slug could not be generated");
		}
		var candidate = base;
		var suffix = 2;
		while (currentPostId == null ? posts.existsBySlug(candidate) : posts.existsBySlugAndIdNot(candidate, currentPostId)) {
			candidate = base + "-" + suffix++;
		}
		return candidate;
	}

	private static String slugify(String value) {
		return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("^-|-$", "");
	}

	private static String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}

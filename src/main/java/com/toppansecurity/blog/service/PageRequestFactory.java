package com.toppansecurity.blog.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PageRequestFactory {
	public static final int MAX_PAGE_SIZE = 100;

	private PageRequestFactory() {
	}

	public static Pageable create(int page, int size, String sortProperty, Sort.Direction direction) {
		if (page < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be zero or greater");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"size must be between 1 and " + MAX_PAGE_SIZE);
		}
		return PageRequest.of(page, size, Sort.by(direction, sortProperty));
	}
}

package com.toppansecurity.blog.model;

public enum PostSort {
	CREATED_AT("createdAt"),
	UPDATED_AT("updatedAt"),
	TITLE("title");

	private final String property;

	PostSort(String property) {
		this.property = property;
	}

	public String property() {
		return property;
	}
}

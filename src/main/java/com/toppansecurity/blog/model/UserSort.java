package com.toppansecurity.blog.model;

public enum UserSort {
	CREATED_AT("createdAt"),
	DISPLAY_NAME("displayName"),
	EMAIL("email");

	private final String property;

	UserSort(String property) {
		this.property = property;
	}

	public String property() {
		return property;
	}
}

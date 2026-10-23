package com.toppansecurity.blog.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toppansecurity.blog.repository.UserRepository;

@Service
public class BlogUserDetailsService implements UserDetailsService {
	private final UserRepository users;

	public BlogUserDetailsService(UserRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		var user = users.findByEmail(email.trim().toLowerCase())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		return User.withUsername(user.getEmail())
				.password(user.getPasswordHash())
				.roles(user.getRole().name())
				.build();
	}
}

package com.toppansecurity.blog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.toppansecurity.blog.model.BlogUser;
import com.toppansecurity.blog.model.Role;

public interface UserRepository extends JpaRepository<BlogUser, Long> {
	Optional<BlogUser> findByEmail(String email);

	boolean existsByEmail(String email);

	Page<BlogUser> findAllByRole(Role role, Pageable pageable);
}

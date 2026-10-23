package com.toppansecurity.blog.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class JwtConfig {
	@Bean
	SecretKey jwtSecretKey(@Value("${blog.jwt.secret}") String secret) {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("BLOG_JWT_SECRET must contain at least 32 bytes");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		var jwk = new OctetSequenceKey.Builder(jwtSecretKey).keyID("blog-signing-key").build();
		var jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}
}

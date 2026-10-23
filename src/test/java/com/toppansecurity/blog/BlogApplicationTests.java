package com.toppansecurity.blog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.profiles.active=test")
class BlogApplicationTests {
	@LocalServerPort
	private int port;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void swaggerAndOpenApiArePublic() throws Exception {
		var client = HttpClient.newHttpClient();
		var openApiResponse = get(client, "/v3/api-docs");
		assertEquals(200, openApiResponse.statusCode());
		JsonNode openApi = objectMapper.readTree(openApiResponse.body());
		assertEquals("bearerAuth", openApi.at("/paths/~1api~1posts/get/security/0").fieldNames().next());
		assertEquals("http", openApi.at("/components/securitySchemes/bearerAuth/type").asText());
		var postListParameters = openApi.at("/paths/~1api~1posts/get/parameters");
		for (String parameter : new String[] { "page", "size", "sort", "direction" }) {
			assertTrue(hasParameter(postListParameters, parameter), "Missing GET query parameter: " + parameter);
		}
		var commentListParameters = openApi.at("/paths/~1api~1posts~1{slug}~1comments/get/parameters");
		for (String parameter : new String[] { "page", "size", "direction" }) {
			assertTrue(hasParameter(commentListParameters, parameter),
					"Missing comment-list query parameter: " + parameter);
		}
		var sortParameter = findParameter(postListParameters, "sort");
		assertNotNull(sortParameter);
		var sortSchema = sortParameter.get("schema");
		assertEquals(3, sortSchema.get("enum").size());
		var loginSecurity = openApi.at("/paths/~1api~1auth~1login/post/security");
		assertTrue(loginSecurity.isMissingNode() || loginSecurity.isEmpty());

		var swaggerResponse = get(client, "/swagger/swagger/index.html");
		assertEquals(302, swaggerResponse.statusCode());
		var redirect = swaggerResponse.headers().firstValue(HttpHeaders.LOCATION).orElse(null);
		assertNotNull(redirect);
		var swaggerUiUri = URI.create(redirect);
		var swaggerUiPath = swaggerUiUri.isAbsolute() ? swaggerUiUri.getRawPath() : redirect;
		assertEquals(200, get(client, swaggerUiPath).statusCode(), swaggerUiPath);
	}

	@Test
	void registrationAndLoginIssueBearerTokenForProtectedEndpoints() throws Exception {
		var client = HttpClient.newHttpClient();
		assertEquals(401, get(client, "/api/posts").statusCode());

		var registerResponse = post(client, "/api/auth/register",
				"""
						{"email":"writer@example.com","displayName":"Writer","password":"correct-horse-battery"}
						""");
		assertEquals(201, registerResponse.statusCode());
		assertEquals("USER", objectMapper.readTree(registerResponse.body()).get("role").asText());

		var loginResponse = post(client, "/api/auth/login",
				"""
						{"email":"writer@example.com","password":"correct-horse-battery"}
						""");
		assertEquals(200, loginResponse.statusCode());
		JsonNode tokenResponse = objectMapper.readTree(loginResponse.body());
		assertEquals("Bearer", tokenResponse.get("tokenType").asText());
		String token = tokenResponse.get("accessToken").asText();
		assertNotNull(token);

		var protectedResponse = client.send(HttpRequest.newBuilder(uri("/api/me/posts"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.GET()
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, protectedResponse.statusCode());

		var pagedResponse = client.send(HttpRequest.newBuilder(
				uri("/api/me/posts?page=0&size=1&sort=TITLE&direction=ASC&status=DRAFT"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.GET()
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, pagedResponse.statusCode());

		var invalidPageSize = client.send(HttpRequest.newBuilder(uri("/api/me/posts?page=0&size=101"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.GET()
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(400, invalidPageSize.statusCode());
	}

	private boolean hasParameter(JsonNode parameters, String name) {
		return findParameter(parameters, name) != null;
	}

	private JsonNode findParameter(JsonNode parameters, String name) {
		for (JsonNode parameter : parameters) {
			if (name.equals(parameter.path("name").asText())) {
				return parameter;
			}
		}
		return null;
	}

	private HttpResponse<String> get(HttpClient client, String path) throws Exception {
		return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(HttpClient client, String path, String json) throws Exception {
		return client.send(HttpRequest.newBuilder(uri(path))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json))
				.build(), HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}
}

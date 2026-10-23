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
		var publicPostsSecurity = openApi.at("/paths/~1api~1posts/get/security");
		assertTrue(publicPostsSecurity.isMissingNode() || publicPostsSecurity.isEmpty());
		assertEquals("bearerAuth",
				openApi.at("/paths/~1api~1me~1posts/get/security/0").fieldNames().next());
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
		var publicPostsResponse = get(client, "/api/posts");
		assertEquals(200, publicPostsResponse.statusCode());
		assertEquals(0, objectMapper.readTree(publicPostsResponse.body()).get("page").get("totalElements").asInt());

		var registerResponse = post(client, "/api/auth/register",
				"""
						{"email":"writer@example.com","displayName":"Writer","password":"correct-horse-battery"}
						""");
		assertEquals(201, registerResponse.statusCode());
		assertEquals("USER", objectMapper.readTree(registerResponse.body()).get("role").asText());

		var invalidRegistration = post(client, "/api/auth/register",
				"""
						{"email":"invalid-password@example.com","displayName":"Invalid Password","password":"Temam@123"}
						""");
		assertEquals(400, invalidRegistration.statusCode());
		JsonNode validationError = objectMapper.readTree(invalidRegistration.body());
		assertEquals("Request validation failed", validationError.get("message").asText());
		assertEquals("password", validationError.get("fieldErrors").get(0).get("field").asText());

		var registerWithStaleToken = client.send(HttpRequest.newBuilder(uri("/api/auth/register"))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-invalid-token")
				.POST(HttpRequest.BodyPublishers.ofString("""
						{"email":"another-writer@example.com","displayName":"Another Writer","password":"correct-horse-battery"}
						"""))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(201, registerWithStaleToken.statusCode());

		var loginResponse = post(client, "/api/auth/login",
				"""
						{"email":"writer@example.com","password":"correct-horse-battery"}
						""");
		assertEquals(200, loginResponse.statusCode());
		JsonNode tokenResponse = objectMapper.readTree(loginResponse.body());
		assertEquals("Bearer", tokenResponse.get("tokenType").asText());
		String token = tokenResponse.get("accessToken").asText();
		assertNotNull(token);

		var createDraftPost = client.send(HttpRequest.newBuilder(uri("/api/posts"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
						{"title":"Draft Access Control Systems","slug":"draft-access-control-systems","content":"Private draft content"}
						"""))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(201, createDraftPost.statusCode());
		assertEquals("DRAFT", objectMapper.readTree(createDraftPost.body()).get("status").asText());
		assertEquals(404, get(client, "/api/posts/draft-access-control-systems").statusCode());

		var createPublishedPost = client.send(HttpRequest.newBuilder(uri("/api/posts"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
						{"title":"Access Control Systems","slug":"importance-of-access-control-systems-2","content":"Publicly available blog content","status":"PUBLISHED"}
						"""))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(201, createPublishedPost.statusCode());

		var publicPostsWithContent = get(client, "/api/posts?page=0&size=20&sort=CREATED_AT&direction=DESC");
		assertEquals(200, publicPostsWithContent.statusCode());
		assertTrue(objectMapper.readTree(publicPostsWithContent.body()).get("content").toString()
				.contains("importance-of-access-control-systems-2"));

		var publicPostBySlug = get(client, "/api/posts/importance-of-access-control-systems-2");
		assertEquals(200, publicPostBySlug.statusCode());
		assertEquals("importance-of-access-control-systems-2",
				objectMapper.readTree(publicPostBySlug.body()).get("slug").asText());
		var publicPostComments = get(client, "/api/posts/importance-of-access-control-systems-2/comments");
		assertEquals(200, publicPostComments.statusCode());

		var createComment = client.send(HttpRequest.newBuilder(
				uri("/api/posts/importance-of-access-control-systems-2/comments"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("""
						{"content":"A comment on the published post"}
						"""))
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(201, createComment.statusCode());

		var protectedResponse = client.send(HttpRequest.newBuilder(uri("/api/me/posts"))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.GET()
				.build(), HttpResponse.BodyHandlers.ofString());
		assertEquals(200, protectedResponse.statusCode());
		JsonNode pagedResponseBody = objectMapper.readTree(protectedResponse.body());
		assertTrue(pagedResponseBody.has("content"));
		assertTrue(pagedResponseBody.has("page"));
		assertTrue(pagedResponseBody.get("page").has("number"));
		assertTrue(pagedResponseBody.get("page").has("size"));
		assertTrue(pagedResponseBody.get("page").has("totalElements"));
		assertTrue(pagedResponseBody.get("page").has("totalPages"));
		assertTrue(!pagedResponseBody.has("pageable"));

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
		assertEquals(400, objectMapper.readTree(invalidPageSize.body()).get("status").asInt());
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

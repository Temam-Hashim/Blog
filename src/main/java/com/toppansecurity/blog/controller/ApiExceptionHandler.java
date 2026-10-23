package com.toppansecurity.blog.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.toppansecurity.blog.dto.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidArguments(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ApiErrorResponse.FieldViolation(error.getField(), error.getDefaultMessage()))
				.sorted(Comparator.comparing(ApiErrorResponse.FieldViolation::field))
				.toList();
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, fieldErrors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiErrorResponse> handleConstraintViolations(ConstraintViolationException exception,
			HttpServletRequest request) {
		var fieldErrors = exception.getConstraintViolations().stream()
				.map(violation -> new ApiErrorResponse.FieldViolation(
						violation.getPropertyPath().toString(), violation.getMessage()))
				.sorted(Comparator.comparing(ApiErrorResponse.FieldViolation::field))
				.toList();
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, fieldErrors);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ApiErrorResponse> handleMethodValidation(HandlerMethodValidationException exception,
			HttpServletRequest request) {
		var fieldErrors = exception.getAllErrors().stream()
				.map(error -> new ApiErrorResponse.FieldViolation("request", error.getDefaultMessage()))
				.sorted(Comparator.comparing(ApiErrorResponse.FieldViolation::field))
				.toList();
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, "Request body is missing or contains invalid data", request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		var expected = exception.getRequiredType() == null ? "the expected type" :
				exception.getRequiredType().getSimpleName();
		return error(HttpStatus.BAD_REQUEST,
				"Parameter '%s' must be a valid %s".formatted(exception.getName(), expected), request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST,
				"Required parameter '%s' is missing".formatted(exception.getParameterName()), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedMethod(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type is not supported for this endpoint", request);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	ResponseEntity<ApiErrorResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_ACCEPTABLE, "The requested response format is not available", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException exception, HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, "Endpoint not found", request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception,
			HttpServletRequest request) {
		var status = HttpStatus.valueOf(exception.getStatusCode().value());
		return error(status, exception.getReason() == null ? status.getReasonPhrase() : exception.getReason(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorResponse> handleDataConflict(DataIntegrityViolationException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, "The request conflicts with existing data", request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception,
			HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled error for {} {}", request.getMethod(), request.getRequestURI(), exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred", request);
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
		return error(status, message, request, List.of());
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request,
			List<ApiErrorResponse.FieldViolation> fieldErrors) {
		var body = new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message,
				request.getRequestURI(), fieldErrors);
		return ResponseEntity.status(status).body(body);
	}
}

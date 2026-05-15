package com.oinkvalley.auth_svc.controller;

import com.oinkvalley.auth_svc.dto.ApiErrorResponse;
import com.oinkvalley.auth_svc.service.DuplicateUserException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class AuthExceptionHandler {

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> badCredentials() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ApiErrorResponse.of("Invalid email or password"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex) {
		List<ApiErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(AuthExceptionHandler::toFieldError)
				.toList();
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse("Validation failed", errors));
	}

	@ExceptionHandler(DuplicateUserException.class)
	public ResponseEntity<ApiErrorResponse> duplicateUser(DuplicateUserException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of(ex.getMessage()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> responseStatus(ResponseStatusException ex) {
		String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
		return ResponseEntity.status(ex.getStatusCode())
				.body(ApiErrorResponse.of(message));
	}

	private static ApiErrorResponse.FieldError toFieldError(FieldError fe) {
		return new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
	}
}

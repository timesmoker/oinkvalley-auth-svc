package com.oinkvalley.auth_svc.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.oinkvalley.auth_svc.service.DuplicateUserException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Void> badCredentials() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Void> validation() {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@ExceptionHandler(DuplicateUserException.class)
	public ResponseEntity<Void> duplicateUser() {
		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}
}

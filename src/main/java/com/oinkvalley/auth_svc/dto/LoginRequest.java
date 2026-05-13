package com.oinkvalley.auth_svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 식별자는 {@link #email}이다. 가입 시 {@code username}은 닉네임으로 저장된다.
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
) {
}

package com.oinkvalley.auth_svc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 요청 본문의 {@code roles}는 **신규 가입에서 무시**된다. 서버가 {@code USER}만 부여한다(향후 관리자 API에서 확장).
 * {@code username}은 **닉네임**(표시용); 계정 식별·로그인은 {@link #email}을 쓴다.
 */
public record SignUpRequest(
		@NotBlank @Email String email,
		@NotBlank String password,
		@NotBlank String username,
		List<String> roles
) {
	public SignUpRequest {
		roles = roles != null ? roles : List.of();
	}
}

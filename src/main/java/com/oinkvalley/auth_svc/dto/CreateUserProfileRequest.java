package com.oinkvalley.auth_svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserProfileRequest(
    @NotNull Long userId, 
    @NotBlank @Size(max = 64) String nickname) {
}
package com.oinkvalley.auth_svc.client;

import com.oinkvalley.auth_svc.dto.CreateUserProfileRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class UserProfileClient {
    
    private final RestClient userProfileRestClient;

    public boolean existsNickname(String nickname) {
        Boolean exists = userProfileRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/profiles/exists")
                        .queryParam("nickname", nickname)
                        .build())
                .retrieve()
                .body(Boolean.class);
    
        return Boolean.TRUE.equals(exists);
    }

    public void createProfile(Long userId, String nickname) {
        userProfileRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/profiles")
                        .build())
                .body(new CreateUserProfileRequest(userId, nickname))
                .retrieve()
                .toBodilessEntity();
    }
}
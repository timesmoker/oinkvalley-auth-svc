package com.oinkvalley.auth_svc.client;

import com.oinkvalley.profile.v1.CreateProfileRequest;
import com.oinkvalley.profile.v1.ExistsNicknameRequest;
import com.oinkvalley.profile.v1.ProfileInternalServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileClient {

	private final ProfileInternalServiceGrpc.ProfileInternalServiceBlockingStub profileStub;

	public boolean existsNickname(String nickname) {
		return profileStub
				.existsNickname(ExistsNicknameRequest.newBuilder().setNickname(nickname).build())
				.getExists();
	}

	public void createProfile(Long userId, String nickname) {
		profileStub.createProfile(
				CreateProfileRequest.newBuilder().setUserId(userId).setNickname(nickname).build());
	}
}

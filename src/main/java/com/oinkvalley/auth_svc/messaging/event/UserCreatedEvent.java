package com.oinkvalley.auth_svc.messaging.event;

/** 계정이 만들어졌다는 사실. 가입·게스트 공통. 이후 동작은 수신자가 정한다. */
public record UserCreatedEvent(long userId) {
}

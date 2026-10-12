package com.oinkvalley.auth_svc.messaging.publisher;

import com.oinkvalley.auth_svc.config.NatsConfig;
import com.oinkvalley.auth_svc.messaging.event.GuestCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 게스트 생성 사실 → {@code auth.guest.created}.
 * 페이로드는 userId. 캘린더·알림 등 후속 작업은 여기 없음.
 */
@Component
public class GuestEventPublisher {

	public static final String SUBJECT = "auth.guest.created";

	private static final Logger log = LoggerFactory.getLogger(GuestEventPublisher.class);

	private final NatsConfig natsConfig;

	public GuestEventPublisher(NatsConfig natsConfig) {
		this.natsConfig = natsConfig;
	}

	public void publish(GuestCreatedEvent event) {
		if (event == null || event.userId() <= 0) {
			return;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("userId", event.userId());
		try {
			natsConfig.publish(SUBJECT, body);
		} catch (Exception e) {
			log.warn("auth.guest.created publish failed userId={}: {}", event.userId(), e.toString());
		}
	}
}

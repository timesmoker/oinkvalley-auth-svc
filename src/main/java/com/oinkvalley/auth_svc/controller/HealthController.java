package com.oinkvalley.auth_svc.controller;

import com.oinkvalley.auth_svc.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	private final HealthService healthService;

	public HealthController(HealthService healthService) {
		this.healthService = healthService;
	}

	@GetMapping("/health")
	public ResponseEntity<String> health() {
		return ResponseEntity.ok(healthService.status());
	}
}

package com.privshield.agent.rest;

import com.privshield.agent.config.AgentProperties;
import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查 REST 端点 — 对应 Python routers/health.py。
 */
@RestController
public class HealthController {

    private final PrivacyService service;
    private final String namespace;

    public HealthController(PrivacyService service, AgentProperties props) {
        this.service = service;
        this.namespace = props.getNamespace();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "namespace", namespace));
    }

    @GetMapping("/v1/health")
    public ResponseEntity<Map<String, String>> v1Health() {
        return ResponseEntity.ok(Map.of("status", "ok", "namespace", namespace));
    }

    @GetMapping("/v1/readyz")
    public ResponseEntity<Map<String, String>> readyz() {
        return ResponseEntity.ok(Map.of("status", "ready"));
    }

    @GetMapping("/livez")
    public ResponseEntity<Map<String, String>> livez() {
        return ResponseEntity.ok(Map.of("status", "alive"));
    }
}

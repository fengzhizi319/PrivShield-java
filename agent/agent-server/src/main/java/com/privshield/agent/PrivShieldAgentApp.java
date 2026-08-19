package com.privshield.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PrivShield Java Agent 入口 — 隐私计算治理边车服务。
 *
 * <p>同时暴露 REST (默认 :8079) 和 gRPC (默认 :50051) 双协议端口，
 * 替代原 Python Agent 为 Go 代理后端及前端提供隐私原语能力。</p>
 */
@SpringBootApplication
public class PrivShieldAgentApp {

    public static void main(String[] args) {
        SpringApplication.run(PrivShieldAgentApp.class, args);
    }
}

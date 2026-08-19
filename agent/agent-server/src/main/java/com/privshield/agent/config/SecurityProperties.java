package com.privshield.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全配置属性 — 映射 application.yml 中 security.* 前缀。
 */
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private boolean authEnabled = false;
    private String apiKey;
    private boolean tlsEnabled = false;
    private boolean rateLimitEnabled = false;
    private int rateLimitRps = 100;

    public boolean isAuthEnabled() { return authEnabled; }
    public void setAuthEnabled(boolean authEnabled) { this.authEnabled = authEnabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isTlsEnabled() { return tlsEnabled; }
    public void setTlsEnabled(boolean tlsEnabled) { this.tlsEnabled = tlsEnabled; }
    public boolean isRateLimitEnabled() { return rateLimitEnabled; }
    public void setRateLimitEnabled(boolean rateLimitEnabled) { this.rateLimitEnabled = rateLimitEnabled; }
    public int getRateLimitRps() { return rateLimitRps; }
    public void setRateLimitRps(int rateLimitRps) { this.rateLimitRps = rateLimitRps; }
}

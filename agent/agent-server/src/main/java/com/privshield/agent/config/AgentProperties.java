package com.privshield.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 配置属性 — 映射 application.yml 中 privacy.* 和 security.* 前缀。
 */
@Component
@ConfigurationProperties(prefix = "privacy")
public class AgentProperties {

    private String namespace = "default";
    private String profilePath;
    private String rulesDir = "rules";
    private String configDir = "config";

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getRulesDir() { return rulesDir; }
    public void setRulesDir(String rulesDir) { this.rulesDir = rulesDir; }
    public String getConfigDir() { return configDir; }
    public void setConfigDir(String configDir) { this.configDir = configDir; }
}

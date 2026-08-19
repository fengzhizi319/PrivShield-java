package com.github.fengzhizi319.privacy.sdk.dynclassification.llm;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.DomainTaxonomy;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.SensitivityLevelDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HTTP-based LLM classifier using OpenAI-compatible API.
 * 基于 HTTP 的 LLM 分类器，使用 OpenAI 兼容 API。
 */
public class HttpLlmClassifier implements LlmClassifier {
    private static final Logger log = LoggerFactory.getLogger(HttpLlmClassifier.class);
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*\"level\"\\s*:\\s*\"([^\"]+)\"[^{}]*}");

    private final String endpoint;
    private final String model;
    private final String systemPrompt;
    private final HttpClient httpClient;
    private final boolean available;

    public HttpLlmClassifier(String endpoint, String model, String systemPrompt) {
        this.endpoint = endpoint;
        this.model = model != null ? model : "default";
        this.systemPrompt = systemPrompt;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.available = validateEndpoint(endpoint);
    }

    private boolean validateEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                log.warn("Invalid LLM endpoint scheme: {}", scheme);
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return false;
            }
            // Block cloud metadata and link-local addresses (SSRF protection)
            if (host.equals("169.254.169.254") || host.startsWith("169.254.")) {
                log.warn("Blocked LLM endpoint (link-local): {}", host);
                return false;
            }
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                // Allow site-local for local development
                if (!addr.isLoopbackAddress() && addr.isLinkLocalAddress()) {
                    log.warn("Blocked LLM endpoint (link-local): {}", host);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Invalid LLM endpoint: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String classify(String fieldName, String fieldValue, DomainTaxonomy taxonomy) {
        if (!available) {
            return null;
        }

        try {
            String prompt = buildPrompt(fieldName, fieldValue, taxonomy);
            String requestBody = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseLevel(response.body(), taxonomy);
            } else {
                log.warn("LLM request failed with status: {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.warn("LLM request failed: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        return null;
    }

    private String buildPrompt(String fieldName, String fieldValue, DomainTaxonomy taxonomy) {
        String levels = taxonomy.getLevels().stream()
            .map(l -> l.getId() + " (" + l.getName() + ")")
            .collect(Collectors.joining(", "));

        String basePrompt = systemPrompt != null ? systemPrompt :
            "You are a data classification assistant. Classify the sensitivity level of the given data.";

        return basePrompt + "\n\nAvailable levels: " + levels +
            "\nField name: " + fieldName +
            "\nField value: " + truncate(fieldValue, 100) +
            "\n\nRespond with JSON: {\"level\": \"L?\"}";
    }

    private String buildRequestBody(String prompt) {
        return "{\"model\":\"" + escapeJson(model) + "\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]," +
            "\"temperature\":0.1,\"max_tokens\":50}";
    }

    private String parseLevel(String responseBody, DomainTaxonomy taxonomy) {
        Matcher matcher = JSON_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            String level = matcher.group(1);
            // Validate level exists in taxonomy
            for (SensitivityLevelDef def : taxonomy.getLevels()) {
                if (def.getId().equalsIgnoreCase(level)) {
                    return def.getId();
                }
            }
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}

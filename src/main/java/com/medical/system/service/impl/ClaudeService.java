package com.medical.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ClaudeService {

    // ★ API Key 在 application.yml 的 claude.api-key 处配置
    @Value("${claude.api-key:}")
    private String apiKey;

    @Value("${claude.model:claude-haiku-4-5-20251001}")
    private String model;

    @Value("${claude.base-url:https://api.anthropic.com}")
    private String baseUrl;

    private static final int MAX_TOKENS = 300;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 检查 API Key 是否已配置
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("YOUR_API_KEY_HERE");
    }

    /**
     * 调用 Claude API 生成文本（仅 user message）
     */
    public String chat(String userPrompt) {
        return chatWithSystem(null, userPrompt, MAX_TOKENS);
    }

    /**
     * 调用 Claude API（支持 System Prompt + 自定义 maxTokens）
     *
     * @param systemPrompt 系统提示词，可为 null
     * @param userMessage  用户消息
     * @param maxTokens    最大输出 token 数
     */
    public String chatWithSystem(String systemPrompt, String userMessage, int maxTokens) {
        if (!isConfigured()) {
            log.warn("Claude API Key 未配置，跳过 AI 调用");
            return null;
        }
        try {
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                body.put("system", systemPrompt);
            }
            body.put("messages", java.util.List.of(
                    java.util.Map.of("role", "user", "content", userMessage)
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/messages"))
                    .timeout(Duration.ofSeconds(30))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Claude API 返回错误 {}：{}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("content").get(0).path("text").asText();

        } catch (Exception e) {
            log.error("调用 Claude API 失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取并解析 JSON 数组
     */
    public <T> T extractJsonArray(String text, TypeReference<T> typeRef) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?s)(\\[.*\\])").matcher(text);
        if (!m.find()) return null;
        try {
            return objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(m.group(1), typeRef);
        } catch (Exception e) {
            log.warn("JSON数组解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取并解析 JSON 对象
     */
    public <T> T extractJsonObject(String text, TypeReference<T> typeRef) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?s)(\\{.*\\})").matcher(text);
        if (!m.find()) return null;
        try {
            return objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(m.group(1), typeRef);
        } catch (Exception e) {
            log.warn("JSON对象解析失败: {}", e.getMessage());
            return null;
        }
    }
}

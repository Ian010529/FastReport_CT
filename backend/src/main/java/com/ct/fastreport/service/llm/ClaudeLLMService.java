package com.ct.fastreport.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeLLMService extends BaseLLMService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeLLMService.class);

    @Value("${llm.claude.api-key:}")
    private String apiKey;

    @Value("${llm.claude.base-url:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${llm.claude.model:claude-3-5-sonnet-latest}")
    private String model;

    @Value("${llm.claude.api-version:2023-06-01}")
    private String apiVersion;

    public ClaudeLLMService(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "claude";
    }

    @Override
    public String generate(LLMGenerationRequest request) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("system", request.systemPrompt());
        body.put("messages", List.of(
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", request.userPrompt())
                ))
        ));
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", apiVersion);

        String url = baseUrl + "/v1/messages";
        log.info("Calling {} provider at {}", provider(), url);

        JsonNode root = readTree(postJson(url, headers, body));
        return root.at("/content/0/text").asText();
    }
}

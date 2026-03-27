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
public class OpenAICompatibleLLMService extends BaseLLMService {

    private static final Logger log = LoggerFactory.getLogger(OpenAICompatibleLLMService.class);

    @Value("${llm.openai.api-key:${openai.api-key:}}")
    private String apiKey;

    @Value("${llm.openai.base-url:${openai.base-url:https://api.openai.com}}")
    private String baseUrl;

    @Value("${llm.openai.model:${openai.model:gpt-4o-mini}}")
    private String model;

    public OpenAICompatibleLLMService(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "openai-compatible";
    }

    @Override
    public String generate(LLMGenerationRequest request) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())
        ));
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        String url = baseUrl + "/v1/chat/completions";
        log.info("Calling {} provider at {}", provider(), url);

        JsonNode root = readTree(postJson(url, headers, body));
        return root.at("/choices/0/message/content").asText();
    }
}

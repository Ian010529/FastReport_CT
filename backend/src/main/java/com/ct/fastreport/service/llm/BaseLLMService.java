package com.ct.fastreport.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public abstract class BaseLLMService implements LLMService {

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;

    protected BaseLLMService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    protected String postJson(String url, HttpHeaders headers, Object body) throws Exception {
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class
        );
        return response.getBody();
    }

    protected JsonNode readTree(String body) throws Exception {
        return objectMapper.readTree(body);
    }
}

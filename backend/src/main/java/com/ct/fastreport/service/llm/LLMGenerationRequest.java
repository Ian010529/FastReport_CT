package com.ct.fastreport.service.llm;

public record LLMGenerationRequest(
        String systemPrompt,
        String userPrompt,
        double temperature,
        int maxTokens
) {
}

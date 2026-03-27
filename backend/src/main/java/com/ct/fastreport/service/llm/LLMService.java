package com.ct.fastreport.service.llm;

public interface LLMService {

    String provider();

    String generate(LLMGenerationRequest request) throws Exception;
}

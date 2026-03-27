package com.ct.fastreport.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class LLMServiceFactory {

    private final Map<String, LLMService> servicesByProvider;
    private final String configuredProvider;

    public LLMServiceFactory(List<LLMService> services,
                             @Value("${llm.provider:openai-compatible}") String configuredProvider) {
        Map<String, LLMService> index = new LinkedHashMap<>();
        for (LLMService service : services) {
            String provider = normalize(service.provider());
            LLMService existing = index.putIfAbsent(provider, service);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate LLM service registration for provider '" + provider + "': " +
                                existing.getClass().getName() + " and " + service.getClass().getName()
                );
            }
        }
        this.servicesByProvider = Map.copyOf(index);
        this.configuredProvider = normalize(configuredProvider);
    }

    public LLMService getService() {
        LLMService service = servicesByProvider.get(configuredProvider);
        if (service == null) {
            throw new IllegalStateException(
                    "No LLM service registered for provider '" + configuredProvider +
                            "'. Supported providers: " + supportedProviders()
            );
        }
        return service;
    }

    public Set<String> supportedProviders() {
        return servicesByProvider.keySet();
    }

    private String normalize(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("LLM provider must not be blank.");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}

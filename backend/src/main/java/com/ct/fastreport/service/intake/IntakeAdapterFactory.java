package com.ct.fastreport.service.intake;

import com.ct.fastreport.exception.ValidationError;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class IntakeAdapterFactory {

    private final Map<String, BaseIntakeAdapter<?, ?>> adaptersBySource;

    public IntakeAdapterFactory(List<BaseIntakeAdapter<?, ?>> adapters) {
        Map<String, BaseIntakeAdapter<?, ?>> adapterIndex = new LinkedHashMap<>();
        for (BaseIntakeAdapter<?, ?> adapter : adapters) {
            String source = normalizeSource(adapter.sourceSystem());
            BaseIntakeAdapter<?, ?> existing = adapterIndex.putIfAbsent(source, adapter);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate intake adapter registration for source '" + source + "': " +
                                existing.getClass().getName() + " and " + adapter.getClass().getName()
                );
            }
        }
        this.adaptersBySource = Map.copyOf(adapterIndex);
    }

    public BaseIntakeAdapter<?, ?> getAdapter(String source) {
        String normalizedSource = normalizeSource(source);
        BaseIntakeAdapter<?, ?> adapter = adaptersBySource.get(normalizedSource);
        if (adapter == null) {
            throw new ValidationError(
                    "UNSUPPORTED_INTAKE_SOURCE",
                    "No intake adapter is registered for the provided source.",
                    Map.of(
                            "source", source,
                            "supportedSources", supportedSources()
                    )
            );
        }
        return adapter;
    }

    public Set<String> supportedSources() {
        return adaptersBySource.keySet();
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            throw new ValidationError("INTAKE_SOURCE_REQUIRED", "source must not be blank.");
        }
        return source.trim().toLowerCase(Locale.ROOT);
    }
}

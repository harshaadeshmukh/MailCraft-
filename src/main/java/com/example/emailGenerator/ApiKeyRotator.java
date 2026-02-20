package com.example.emailGenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiKeyRotator {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyRotator.class);

    private final List<String> apiKeys;
    private final AtomicInteger counter = new AtomicInteger(0);

    public ApiKeyRotator(@Value("${gemini.api.keys}") String keysRaw) {
        this.apiKeys = Arrays.stream(keysRaw.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList();

        if (this.apiKeys.isEmpty()) {
            throw new IllegalStateException("No Gemini API keys configured!");
        }
        log.info("✅ ApiKeyRotator initialized with {} keys", this.apiKeys.size());
    }

    public String getNextKey() {
        int index = Math.abs(counter.getAndIncrement() % apiKeys.size());
        log.info("🔑 Using key index {}", index);
        return apiKeys.get(index);
    }

    public int getTotalKeys() {
        return apiKeys.size();
    }
}
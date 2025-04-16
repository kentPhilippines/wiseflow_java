package com.wiseflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;

@Configuration
public class AiConfig {
    
    @Value("${openai.api.key}")
    private String openaiApiKey;
    
    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(openaiApiKey, Duration.ofSeconds(60));
    }
} 
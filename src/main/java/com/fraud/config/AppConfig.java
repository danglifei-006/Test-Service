package com.fraud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用程序配置类，配置通用的Bean组件
 */
@Configuration
public class AppConfig {

    /**
     * 创建Jackson ObjectMapper用于JSON序列化和反序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

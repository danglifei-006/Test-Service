package com.fraud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 LocalDateTime、Instant 等类型
        objectMapper.registerModule(new JavaTimeModule());
        // 可选：关闭日期时间的序列化格式（默认会序列化为时间戳，关闭后用 ISO 格式）
         objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}

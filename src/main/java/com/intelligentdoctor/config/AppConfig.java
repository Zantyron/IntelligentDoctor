package com.intelligentdoctor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.intelligentdoctor.admin.security.AdminAuthInterceptor;
import com.intelligentdoctor.auth.TerminalAuthInterceptor;
import com.intelligentdoctor.chat.security.ChatRateLimitInterceptor;
import com.intelligentdoctor.tenant.TenantContextInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final TerminalAuthInterceptor terminalAuthInterceptor;
    private final ChatRateLimitInterceptor chatRateLimitInterceptor;
    private final TenantContextInterceptor tenantContextInterceptor;
    private final AppProperties properties;

    public AppConfig(AdminAuthInterceptor adminAuthInterceptor,
                     TerminalAuthInterceptor terminalAuthInterceptor,
                     ChatRateLimitInterceptor chatRateLimitInterceptor,
                     TenantContextInterceptor tenantContextInterceptor,
                     AppProperties properties) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.terminalAuthInterceptor = terminalAuthInterceptor;
        this.chatRateLimitInterceptor = chatRateLimitInterceptor;
        this.tenantContextInterceptor = tenantContextInterceptor;
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");
        registry.addInterceptor(terminalAuthInterceptor)
                .addPathPatterns("/api/chat/**", "/api/registration/**");
        registry.addInterceptor(chatRateLimitInterceptor)
                .addPathPatterns("/api/chat/diagnosis/stream", "/api/chat/registration/stream");
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("intelligent-doctor-");
        executor.setCorePoolSize(properties.getExecutor().getCorePoolSize());
        executor.setMaxPoolSize(properties.getExecutor().getMaxPoolSize());
        executor.setQueueCapacity(properties.getExecutor().getQueueCapacity());
        executor.initialize();
        return executor;
    }
}

package com.intelligentdoctor.chat.security;

import com.intelligentdoctor.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ChatRateLimitInterceptor implements HandlerInterceptor {

    private final AppProperties properties;
    private final Clock clock;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Autowired
    public ChatRateLimitInterceptor(AppProperties properties) {
        this(properties, Clock.systemUTC());
    }

    ChatRateLimitInterceptor(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }
        String key = clientKey(request);
        long now = clock.millis();
        long windowMillis = properties.getRateLimit().getWindowSeconds() * 1000;
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.expiresAtMillis()) {
                return new WindowCounter(now + windowMillis);
            }
            return existing;
        });
        int used = counter.count().incrementAndGet();
        if (used <= properties.getRateLimit().getMaxRequests()) {
            return true;
        }
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(properties.getRateLimit().getWindowSeconds()));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        return false;
    }

    private String clientKey(HttpServletRequest request) {
        String sessionId = request.getHeader("X-Session-Id");
        if (sessionId != null && !sessionId.isBlank()) {
            return "session:" + sessionId;
        }
        return "ip:" + request.getRemoteAddr();
    }

    private record WindowCounter(long expiresAtMillis, AtomicInteger count) {
        private WindowCounter(long expiresAtMillis) {
            this(expiresAtMillis, new AtomicInteger());
        }
    }
}

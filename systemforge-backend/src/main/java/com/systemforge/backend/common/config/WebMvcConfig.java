package com.systemforge.backend.common.config;

import com.systemforge.backend.common.filter.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration — registers interceptors for specific path patterns.
 *
 * <p>Rate limiting is applied only to endpoints that are either:
 * <ul>
 *   <li>Public and abuse-prone (OTP, login)</li>
 *   <li>Authenticated but resource-expensive (AI generation)</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/otp/**",
                        "/api/v1/auth/login",
                        "/api/v1/systems/configs/*/generate",
                        "/api/v1/architect/chat"
                );
    }
}

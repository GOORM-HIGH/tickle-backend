package com.profect.tickle.global.config;

import com.profect.tickle.domain.chat.config.ChatJwtAuthenticationInterceptor;
import com.profect.tickle.domain.chat.resolver.CurrentMemberArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ChatJwtAuthenticationInterceptor chatJwtAuthenticationInterceptor;
    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ✅ 채팅 API만 타겟팅 - 다른 기능에 영향 없음
        registry.addInterceptor(chatJwtAuthenticationInterceptor)
                .addPathPatterns("/api/v1/chat/**", "/api/v1/files/**")
                .excludePathPatterns("/api/v1/websocket/**"); // WebSocket 정보 API는 제외
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }
}

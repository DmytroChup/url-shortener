package com.chupryna.url_shortener.interceptor;

import com.chupryna.url_shortener.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws
            Exception {
        String clientIp = extractClientIp(request);
        boolean isWrite = "POST".equalsIgnoreCase(request.getMethod());

        boolean allowed = isWrite
                ? rateLimitingService.tryConsumeWrite(clientIp)
                : rateLimitingService.tryConsumeRead(clientIp);

        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("""
                    {
                        "title": "Too Many Requests",
                        "status": 429,
                        "detail": "Rate limit exceeded. Try again later."
                    }
                """);
            return false;
        }

        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}

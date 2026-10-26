package com.chupryna.gateway.filter;

import com.chupryna.gateway.util.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^/[a-zA-Z0-9]+$");

    private final JwtValidator jwtValidator;
    private final ObjectMapper objectMapper;

    private final PathPatternParser pathParser = new PathPatternParser();
    private final List<PathPattern> strictlyPublicPatterns = List.of(
            pathParser.parse("/api/v1/auth/**"),
            pathParser.parse("/actuator/health"),
            pathParser.parse("/fallback/**")
    );
    private final PathPattern shortenPattern = pathParser.parse("/api/v1/urls/shorten");
    private final PathPattern shortenTrailingPattern = pathParser.parse("/api/v1/urls/shorten/");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        PathContainer path = request.getPath().pathWithinApplication();
        HttpMethod method = request.getMethod();

        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                });
        boolean strictlyPublic = isStrictlyPublicPath(path, method);
        boolean guestAllowed = isGuestShortenAllowed(path, method);

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = jwtValidator.validateAndExtractClaims(token);
                requestBuilder.header("X-User-Id", claims.getSubject());
                String role = claims.get("role", String.class);

                if (role != null) {
                    requestBuilder.header("X-User-Role", role);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("JWT validation failed for path {}: {}", path.value(), ex.getMessage());
                if (!strictlyPublic && !guestAllowed) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
                }
            }
        } else {
            if (!strictlyPublic && !guestAllowed) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource");
            }
        }
        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    private boolean isStrictlyPublicPath(PathContainer path, HttpMethod method) {
        for (PathPattern pattern : strictlyPublicPatterns) {
            if (pattern.matches(path)) {
                return true;
            }
        }
        return HttpMethod.GET.equals(method) && SHORT_CODE_PATTERN.matcher(path.value()).matches();
    }

    private boolean isGuestShortenAllowed(PathContainer path, HttpMethod method) {
        return HttpMethod.POST.equals(method) &&
            (shortenPattern.matches(path) || shortenTrailingPattern.matches(path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(exchange.getRequest().getPath().value()));

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsBytes(problemDetail);
        } catch (Exception e) {
            bytes = ("{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase() + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

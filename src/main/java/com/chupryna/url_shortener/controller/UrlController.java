package com.chupryna.url_shortener.controller;

import com.chupryna.url_shortener.dto.UrlAnalyticsResponse;
import com.chupryna.url_shortener.dto.UrlRequest;
import com.chupryna.url_shortener.event.UrlClickEvent;
import com.chupryna.url_shortener.service.UrlAnalyticsService;
import com.chupryna.url_shortener.service.UrlService;
import com.chupryna.url_shortener.util.IpMasker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController()
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Shortener", description = "Endpoints for managing short URLs and redirects")
public class UrlController {

    private final UrlService urlService;
    private final UrlAnalyticsService urlAnalyticsService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final IpMasker ipMasker;

    @PostMapping("/shorten")
    @Operation(
            summary = "Shorten a long URL",
            description = "Generates a 7-character random alphanumeric short code, persists it in PostgreSQL, " +
                    "and warms up the Redis cache."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL shortened successfully, returns short code"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request payload")
    })
    public ResponseEntity<String> shortenUrl(@RequestBody @Valid UrlRequest urlRequest) {
        String shortCode = urlService.shortenUrl(urlRequest.url(), urlRequest.ttlDays());
        return ResponseEntity.ok(shortCode);
    }


    @GetMapping("/{shortCode:[a-zA-Z0-9]{7}}")
    @Operation(
            summary = "Redirect to original URL",
            description = "Resolve original URL from Redis cache (or PostgreSQL on cache miss) " +
                    "and performs HTTP 302 redirect."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Redirect to target URL"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Unique Base62 short code", example = "aB7xK9q")
            @PathVariable String shortCode,
            HttpServletRequest request
    ) {
        String originalUrl = urlService.getOriginalUrl(shortCode);
        String rawIp = extractClientIp(request);
        String maskedIp = ipMasker.mask(rawIp);

        URI target;
        try {
            target = URI.create(originalUrl);
        } catch (IllegalArgumentException e) {
            log.error("Corrupted URL in database for shortCode {}: {}", shortCode, originalUrl, e);
            throw new IllegalStateException("Stored URL is malformed");
        }

        applicationEventPublisher.publishEvent(new UrlClickEvent(
                shortCode,
                request.getHeader("User-Agent"),
                maskedIp,
                Instant.now(),
                request.getHeader("Referer"))
        );
        
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(target)
                .build();
    }

    // TODO: add owner authorization
    @GetMapping("/{shortCode:[a-zA-Z0-9]{7}}/analytics")
    @Operation(
            summary = "Get click analytics for short URL",
            description = "Returns total click count, short code, and destination original URL. " +
                    "Throws 404 if the code does not exist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Short URL not found")
    })
    public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(
            @Parameter(description = "Unique Base62 short code", example = "aB7xK9q")
            @PathVariable String shortCode
    ) {
        return ResponseEntity.ok(urlAnalyticsService.getAnalytics(shortCode));
    }

    private String extractClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}

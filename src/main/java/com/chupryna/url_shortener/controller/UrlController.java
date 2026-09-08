package com.chupryna.url_shortener.controller;

import com.chupryna.url_shortener.dto.UrlRequest;
import com.chupryna.url_shortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController()
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Endpoints for managing short URLs and redirects")
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    @Operation(
            summary = "Shorten a long URL",
            description = "Encodes URL using Base62, persists it in PostgreSQL, and warms up the Redis cache."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URL shortened successfully, returns short code"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request payload")
    })
    public ResponseEntity<String> shortenUrl(@RequestBody @Valid UrlRequest urlRequest) {
        String shortCode = urlService.shortenUrl(urlRequest.url());
        return ResponseEntity.ok(shortCode);
    }


    @GetMapping("/{shortCode}")
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
            @Parameter(description = "Unique Base62 short code", example = "a")
            @PathVariable String shortCode
    ) {
        String originalUrl = urlService.getOriginalUrl(shortCode);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}

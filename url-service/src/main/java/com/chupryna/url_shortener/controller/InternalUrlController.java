package com.chupryna.url_shortener.controller;

import com.chupryna.url_shortener.dto.UrlAccessResponse;
import com.chupryna.url_shortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/api/v1/urls")
@RequiredArgsConstructor
@Tag(name = "Internal URL", description = "Internal inter-service endpoints for access control")
public class InternalUrlController {

    private final UrlService urlService;

    @GetMapping("/{shortCode:[a-zA-Z0-9]{7}}/access")
    @Operation(summary = "Check if user has access to the URL", description = "Internal endpoint called by analytics-service")
    public ResponseEntity<UrlAccessResponse> checkAccess(
        @Parameter(description = "Unique Base62 short code", example = "aB7xK9q")
        @PathVariable String shortCode,
        @Parameter(description = "User ID", example = "52")
        @RequestParam Long userId
    ) {
        boolean allowed = urlService.checkAccess(shortCode, userId);
        return ResponseEntity.ok(new UrlAccessResponse(allowed));
    }
}

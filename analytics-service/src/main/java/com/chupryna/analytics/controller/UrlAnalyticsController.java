package com.chupryna.analytics.controller;

import com.chupryna.analytics.client.UrlAccessClient;
import com.chupryna.analytics.dto.UrlAnalyticsResponse;
import com.chupryna.analytics.exception.AccessDeniedException;
import com.chupryna.analytics.service.UrlAnalyticsService;
import com.chupryna.url_shortener.security.SecurityHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for viewing URL click metrics")
public class UrlAnalyticsController {

    private final UrlAnalyticsService urlAnalyticsService;
    private final UrlAccessClient urlAccessClient;

    @GetMapping("/{shortCode:[a-zA-Z0-9]{7}}")
    @Operation(
        summary = "Get click analytics for short URL",
        description = "Returns total click count for the given 7-character short code."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied or resource not found")
    })
    public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(
        @Parameter(description = "Unique Base62 short code", example = "aB7xK9q")
        @PathVariable String shortCode,
        @Parameter(hidden = true)
        @RequestHeader(value = SecurityHeaders.X_USER_ID, required = false) Long userId,
        @Parameter(hidden = true)
        @RequestHeader(value = SecurityHeaders.X_USER_ROLE, required = false) String userRole
    ) {
        if(userId == null) {
            throw new AccessDeniedException("Access denied or resource not found");
        }

        boolean isAdmin = SecurityHeaders.ROLE_ADMIN.equals(userRole);
        if(!isAdmin && !urlAccessClient.hasAccess(shortCode, userId)) {
            throw new AccessDeniedException("Access denied or resource not found");
        }

        return ResponseEntity.ok(urlAnalyticsService.getAnalytics(shortCode));
    }
}

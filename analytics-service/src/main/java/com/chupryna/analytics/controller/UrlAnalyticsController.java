package com.chupryna.analytics.controller;

import com.chupryna.analytics.dto.UrlAnalyticsResponse;
import com.chupryna.analytics.service.UrlAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for viewing URL click metrics")
public class UrlAnalyticsController {

    private final UrlAnalyticsService urlAnalyticsService;

    // TODO: add owner authorization
    @GetMapping("/{shortCode:[a-zA-Z0-9]{7}}")
    @Operation(
            summary = "Get click analytics for short URL",
            description = "Returns total click count for the given 7-character short code."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analytics retrieved successfully")
    })
    public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(
            @Parameter(description = "Unique Base62 short code", example = "aB7xK9q")
            @PathVariable String shortCode
    ) {
        return ResponseEntity.ok(urlAnalyticsService.getAnalytics(shortCode));
    }
}

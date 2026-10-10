package com.chupryna.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/analytics")
    public Mono<ResponseEntity<ProblemDetail>> analyticsFallback() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Analytics service is temporarily unavailable. Please try again later."
        );
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setType(URI.create("urn:problem-type:service-unavailable"));
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail));
    }
}

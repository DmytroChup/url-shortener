package com.chupryna.url_shortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_clicks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String shortCode;

    @Column(updatable = false, nullable = false)
    private LocalDateTime clickedAt;

    @Column(length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String maskedIpAddress;

    @Column(columnDefinition = "TEXT")
    private String referer;
}

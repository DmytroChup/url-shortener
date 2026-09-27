package com.chupryna.url_shortener.repository;

import com.chupryna.url_shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Url u WHERE u.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);

    @Query("SELECT u.shortCode FROM Url u WHERE u.expiresAt < :cutoff")
    List<String> findShortCodesExpiredBefore(@Param("cutoff") Instant cutoff);
}

package com.chupryna.url_shortener.repository;

import com.chupryna.url_shortener.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {

    long countByShortCode(String shortCode);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UrlClick c WHERE c.clickedAt < :threshold")
    int deleteByClickedAtBefore(@Param("threshold") LocalDateTime threshold);
}

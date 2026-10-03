package com.chupryna.analytics.repository;

import com.chupryna.analytics.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {

    long countByShortCode(String shortCode);

    boolean existsByEventId(UUID eventId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UrlClick c WHERE c.shortCode IN :shortCodes")
    int deleteByShortCodeIn(@Param("shortCodes") List<String> shortCodes);
}

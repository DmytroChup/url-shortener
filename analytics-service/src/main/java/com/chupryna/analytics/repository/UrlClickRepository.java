package com.chupryna.analytics.repository;

import com.chupryna.analytics.entity.UrlClick;
import org.springframework.data.domain.Pageable;
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

    public interface MetricCount {
        String getName();
        Long getTotal();
    }

    @Query("""
                SELECT c.countryCode AS name, COUNT(c) AS total
                FROM UrlClick c
                WHERE c.shortCode = :shortCode AND c.countryCode IS NOT NULL
                GROUP BY c.countryCode
                ORDER BY COUNT(c) DESC, c.countryCode ASC
                """)
    List<MetricCount> findTopCountries(@Param("shortCode") String shortCode, Pageable pageable);

    @Query("""
                SELECT c.deviceType AS name, COUNT(c) AS total
                FROM UrlClick c
                WHERE c.shortCode = :shortCode AND c.deviceType IS NOT NULL
                GROUP BY c.deviceType
                ORDER BY COUNT(c) DESC, c.deviceType ASC
                """)
    List<MetricCount> findDevices(@Param("shortCode") String shortCode, Pageable pageable);

    @Query("""
                SELECT c.browser AS name, COUNT(c) AS total
                FROM UrlClick c
                WHERE c.shortCode = :shortCode AND c.browser IS NOT NULL
                GROUP BY c.browser
                ORDER BY COUNT(c) DESC, c.browser ASC
                """)
    List<MetricCount> findTopBrowsers(@Param("shortCode") String shortCode, Pageable pageable);

    @Query("""
                SELECT c.operatingSystem AS name, COUNT(c) AS total
                FROM UrlClick c
                WHERE c.shortCode = :shortCode AND c.operatingSystem IS NOT NULL
                GROUP BY c.operatingSystem
                ORDER BY COUNT(c) DESC, c.operatingSystem ASC
                """)
    List<MetricCount> findTopOperatingSystems(@Param("shortCode") String shortCode, Pageable pageable);
}

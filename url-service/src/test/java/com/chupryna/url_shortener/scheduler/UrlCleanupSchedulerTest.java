package com.chupryna.url_shortener.scheduler;

import com.chupryna.url_shortener.repository.UrlClickRepository;
import com.chupryna.url_shortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UrlCleanupSchedulerTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UrlClickRepository urlClickRepository;

    @InjectMocks
    private UrlCleanupScheduler scheduler;

    @Test
    @DisplayName("Should delete click records before deleting expired urls, in correct order")
    void purgeExpiredLinks_DeletesClicksBeforeUrls() {
        List<String> expiredCodes = List.of("aB7xK9q", "Zt3pQ8m");

        when(urlRepository.findShortCodesExpiredBefore(any())).thenReturn(expiredCodes);
        when(urlClickRepository.deleteByShortCodeIn(expiredCodes)).thenReturn(15);
        when(urlRepository.deleteByExpiresAtBefore(any())).thenReturn(2);

        scheduler.purgeExpiredLinks();

        InOrder inOrder = inOrder(urlClickRepository, urlRepository);
        inOrder.verify(urlClickRepository).deleteByShortCodeIn(expiredCodes);
        inOrder.verify(urlRepository).deleteByExpiresAtBefore(any());
    }

    @Test
    @DisplayName("Should skip deletion entirely when no expired links found")
    void purgeExpiredLinks_NoExpiredLinks_SkipsDeletion() {
        when(urlRepository.findShortCodesExpiredBefore(any())).thenReturn(List.of());

        scheduler.purgeExpiredLinks();

        verify(urlClickRepository, never()).deleteByShortCodeIn(any());
        verify(urlRepository, never()).deleteByExpiresAtBefore(any());
    }
}

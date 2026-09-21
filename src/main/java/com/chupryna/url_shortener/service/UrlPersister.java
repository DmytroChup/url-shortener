package com.chupryna.url_shortener.service;

import com.chupryna.url_shortener.entity.Url;
import com.chupryna.url_shortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UrlPersister {

    private final UrlRepository urlRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Url persist(Url url) {
        return urlRepository.saveAndFlush(url);
    }
}

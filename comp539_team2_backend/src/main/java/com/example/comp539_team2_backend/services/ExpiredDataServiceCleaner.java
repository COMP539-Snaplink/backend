package com.example.comp539_team2_backend.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ExpiredDataServiceCleaner {

    private final UrlShorteningService urlShorteningService;

    public ExpiredDataServiceCleaner(UrlShorteningService urlShorteningService) {
        this.urlShorteningService = urlShorteningService;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpirationData() {

    }

}

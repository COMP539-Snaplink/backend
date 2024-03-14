package com.example.comp539_team2_backend.controllers;

import com.example.comp539_team2_backend.services.UrlShorteningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.comp539_team2_backend.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UrlShorteningController {
    @Autowired
    private UrlShorteningService urlShorteningService;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody UrlRequestDTO request) {
        try {
            String shortened_url = urlShorteningService.shorten_url(request.getLongUrl());
            return ResponseEntity.ok(shortened_url);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to shorten url: " + e.getMessage());
        }
    }

    @GetMapping("/resolve/{short_url}")
    public ResponseEntity<String> resolveUrl(@PathVariable String short_url) {
        try {
            String original_url = urlShorteningService.resolve_url(short_url);
            if (original_url != null) {
                return ResponseEntity.ok(original_url);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to shorten url: " + e.getMessage());
        }
    }

    @PostMapping("/bulk_shorten")
    public ResponseEntity<List<String>> bulk_shorten_url(@RequestBody UrlRequestDTO request) {
        try {
            List<String> shortened_Urls = urlShorteningService.bulk_shorten_urls(request.getLongUrls(), "");
            return ResponseEntity.ok(shortened_Urls);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Failed to shorten urls: " + e.getMessage()));
        }
    }

    @GetMapping("/bulk_resolve")
    public ResponseEntity<List<String>> bulkResolveUrls(@RequestBody List<String> shortUrls) {
        try {
            List<String> originalUrls = urlShorteningService.bulk_resolve_urls(shortUrls.toArray(new String[0]), "");
            return ResponseEntity.ok(originalUrls);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Failed to resolve urls: " + e.getMessage()));
        }
    }

}

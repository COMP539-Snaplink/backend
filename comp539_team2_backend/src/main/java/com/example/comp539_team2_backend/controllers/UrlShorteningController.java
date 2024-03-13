package com.example.comp539_team2_backend.controllers;

import com.example.comp539_team2_backend.services.UrlShorteningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.comp539_team2_backend.*;

@RestController
@RequestMapping("/api")
public class UrlShorteningController {
    @Autowired
    private UrlShorteningService urlShorteningService;

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody UrlRequestDTO request) {
        System.out.println("request = " + request.getLongUrl());
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
}

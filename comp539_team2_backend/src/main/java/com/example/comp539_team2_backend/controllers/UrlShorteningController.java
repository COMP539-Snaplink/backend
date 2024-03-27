package com.example.comp539_team2_backend.controllers;

import com.example.comp539_team2_backend.configs.BigtableRepository;
import com.example.comp539_team2_backend.entities.UserInfo;
import com.example.comp539_team2_backend.services.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.comp539_team2_backend.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class UrlShorteningController {
    @Autowired
    private UrlShorteningService urlShorteningService;

    @Autowired
    private UserInfoService userInfoService;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UrlShorteningService.class);

    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@RequestBody UrlRequestDTO request) {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String shortened_url = urlShorteningService.shorten_url(request.getLongUrl(), key);
            return ResponseEntity.ok(shortened_url);
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.internalServerError().body("Failed to shorten url: " + errorMessage);
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
            return ResponseEntity.internalServerError().body("Failed to resolve url: " + e.getCause().getMessage());
        }
    }

//    @GetMapping("/bulk_resolve")
//    public ResponseEntity<List<String>> bulkResolveUrls(@RequestBody UrlRequestDTO request) throws IOException {
//        try {
//            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
//            List<String> originalUrls = urlShorteningService.bulk_resolve_urls(request.getShortUrls(), key);
//            return ResponseEntity.ok(originalUrls);
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().body(Collections.singletonList("Failed to resolve urls: " + e.getMessage()));
//        }
//    }

    @PostMapping("/bulk_shorten")
    public ResponseEntity<List<String>> bulk_shorten_url(@RequestBody UrlRequestDTO request) {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            List<String> shortened_Urls = urlShorteningService.bulk_shorten_urls(request.getLongUrls(), key);
            return ResponseEntity.ok(shortened_Urls);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Failed to shorten urls: " + e.getMessage()));
        }
    }


    @PostMapping("/bulk_resolve")
    public ResponseEntity<List<String>> bulk_resolve_url(@RequestBody UrlRequestDTO request) {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            List<String> originalUrls = urlShorteningService.bulk_resolve_urls(request.getShortUrls(), key);
            return ResponseEntity.ok(originalUrls);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Failed to shorten urls: " + e.getMessage()));
        }
    }

    @PutMapping("/renew_expiration")
    public ResponseEntity<String> renewExpiration(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String result = urlShorteningService.renew_url_expiration(key) ? "Success to update." : "Fail to update.";
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update url expiration: " + e.getCause().getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteShortenedUrl(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String short_url = request.getShortUrl();
            String result = urlShorteningService.delete_url(short_url, key) ? "Success to delete." : "Unauthorized to delete.";
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete url: " + e.getCause().getMessage());
        }
    }

    @PostMapping("/customizeUrl")
    public ResponseEntity<String> customizedUrl(@RequestBody UrlRequestDTO request) {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String shortened_url = urlShorteningService.customized_url(request.getLongUrl(), request.getShortUrl(), key);
            return ResponseEntity.ok(shortened_url);
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.internalServerError().body("Failed to curtomized url: " + errorMessage);
        }
    }



}

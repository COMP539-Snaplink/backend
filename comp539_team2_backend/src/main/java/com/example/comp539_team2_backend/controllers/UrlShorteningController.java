package com.example.comp539_team2_backend.controllers;

import com.example.comp539_team2_backend.configs.BigtableRepository;
import com.example.comp539_team2_backend.entities.UserInfo;
import com.example.comp539_team2_backend.services.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.comp539_team2_backend.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import com.example.comp539_team2_backend.JSONResult;

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
    public ResponseEntity<JSONResult<Map<String, String>>> shortenUrl(@RequestBody UrlRequestDTO request) {
        long startTime = System.currentTimeMillis();
        Map<String, String> result = new HashMap<>();

        if (request.getLongUrl() == null || request.getLongUrl().isEmpty()) {
            logger.error("Invalid URL provided");
            return ResponseEntity.badRequest().body(new JSONResult<>("Invalid URL", null));
        }

        try {
            String key = (request.getEmail() == null || request.getEmail().isEmpty()) ? "NO_USER" : request.getEmail();
            String shortenedUrl = urlShorteningService.shorten_url(request.getLongUrl(), key);
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            result.put("latency", String.valueOf(latency));
            result.put("shortenedUrl", shortenedUrl);
            logger.info("URL shortened successfully");
            return ResponseEntity.ok(new JSONResult<>("success", result));
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            result.put("errorMessage", errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONResult<>("error", result));
        }
    }

    @GetMapping("/redirect/{short_url}")
    public RedirectView redirectUrl(@PathVariable("short_url") String shortUrl) {
        try {
            String originalUrl = urlShorteningService.resolve_url(shortUrl);
            if (originalUrl != null) {
                return new RedirectView(originalUrl);
            } else {
                return new RedirectView("/not-found", true);
            }
        } catch (Exception e) {
            RedirectView redirectView = new RedirectView("/error");
            redirectView.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return redirectView;
        }
    }

    @GetMapping("/resolve/{short_url}")
    public ResponseEntity<JSONResult<String>> resolveUrl(@PathVariable("short_url") String shortUrl) {
        try {
            String originalUrl = urlShorteningService.resolve_url(shortUrl);
            if (originalUrl != null) {
                return ResponseEntity.ok(new JSONResult<>("success", originalUrl));
            } else {
                logger.info("No URL found for the provided short URL: {}", shortUrl);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONResult<>("error", null));
            }
        } catch (Exception e) {
            logger.error("Error resolving URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONResult<>("Error resolving URL", null));
        }
    }

    @PostMapping("/bulk_shorten")
    public ResponseEntity<JSONResult<Map<String, Object>>> bulkShortenUrl(@RequestBody UrlRequestDTO request) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            List<String> shortenedUrls = urlShorteningService.bulk_shorten_urls(request.getLongUrls(), key);
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            result.put("shortenedUrls", shortenedUrls);
            result.put("latency", latency);
            return ResponseEntity.ok(new JSONResult<>("success", result));
        } catch (Exception e) {
            result.put("error", "Failed to shorten URLs: " + e.getMessage());
            return ResponseEntity.internalServerError().body(new JSONResult<>("error", result));
        }
    }



    @PostMapping("/bulk_resolve")
    public ResponseEntity<JSONResult<List<String>>> bulk_resolve_url(@RequestBody UrlRequestDTO request) {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            List<String> originalUrls = urlShorteningService.bulk_resolve_urls(request.getShortUrls(), key);
            return ResponseEntity.ok(new JSONResult<>("success", originalUrls));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new JSONResult<>("error", Collections.singletonList("Failed to shorten urls: " + e.getMessage())));
        }
    }

    @PutMapping("/renew_expiration")
    public ResponseEntity<JSONResult<String>> renewExpiration(@RequestBody UrlRequestDTO request) {
        String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
        try {
            if (urlShorteningService.renew_url_expiration(key)) {
                return ResponseEntity.ok(new JSONResult<>("success", "Success to update url expiration"));
            } else {
                logger.info("Update failed, no permission: {}", key);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONResult<>("error", "Forbidden: You do not have permission to update this URL"));
            }
        } catch (Exception e) {
            logger.error("Error updating URL expiration for key {}: {}", key, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new JSONResult<>("error", "Failed to update URL expiration"));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<JSONResult<String>> deleteShortenedUrl(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String short_url = request.getShortUrl();
            String result = "";
            if (urlShorteningService.delete_url(short_url, key)) {
                result = "Success to delete.";
                return ResponseEntity.ok(new JSONResult<>("success", result));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONResult<>("error", "Forbidden: You do not have permission to delete this URL"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new JSONResult<>("error", "Failed to delete url: " + e.getCause().getMessage()));
        }
    }

    @PostMapping("/customizeUrl")
    public ResponseEntity<JSONResult<String>> customizedUrl(@RequestBody UrlRequestDTO request) {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String shortened_url = urlShorteningService.customized_url(request.getLongUrl(), request.getShortUrl(), key);
            return ResponseEntity.ok(new JSONResult<>("success", shortened_url));
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.internalServerError().body(new JSONResult<>("error", "Failed to customized url: " + errorMessage));
        }
    }

    @GetMapping("/_ah/warmup")
    public String warmup() {
        System.out.println("Warmup request received.");
        return "Warmup done";
    }
}

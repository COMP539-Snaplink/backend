package com.example.comp539_team2_backend.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.comp539_team2_backend.UrlRequestDTO;
import com.example.comp539_team2_backend.services.UrlShorteningService;
import com.example.comp539_team2_backend.services.UserInfoService;

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
        long startTime = System.currentTimeMillis();
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String shortened_url = urlShorteningService.shorten_url(request.getLongUrl(), key);
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            System.out.println("API latency for /api/shorten = " + latency + "ms");
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

    @PostMapping("/markSpam")
    public ResponseEntity<String> markUrlAsSpam(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            String short_url = request.getShortUrl();
            String email = request.getEmail();
            String result = urlShorteningService.mark_url_as_spam(short_url,email) ? "Success to mark as spam." : "unable to mark as spam.";
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to mark url as spam: " + e.getCause().getMessage());
        }
        
    }
    @PostMapping("/removeSpam")
    public ResponseEntity<String> removeSpam(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            String short_url = request.getShortUrl();
            String email = request.getEmail();
            String result = urlShorteningService.remove_spam(short_url,email) ? "Successfully removed spam" : "unable to remove spam.";
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete url: " + e.getCause().getMessage());
        }
        
    }
    @PostMapping("/getInfo")
    public ResponseEntity<Map<String, String>> getInfo(@RequestBody UrlRequestDTO request) throws IOException {
        try {
             Map<String, String> information 
            = new HashMap<String,String>(); 
            String short_url = request.getShortUrl();
            String email = request.getEmail();
            information = urlShorteningService.get_info(short_url,email);
            return ResponseEntity.ok(information);
        } 
        catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get information: " + e.getCause().getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
        
    }
    @PostMapping("/getHistory")
    public ResponseEntity<List<String>> getHistory(@RequestBody UrlRequestDTO request) throws IOException {
        try {
          
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            List<String> information = urlShorteningService.get_history(key);
            return ResponseEntity.ok(information);
        } 
        catch (Exception e) {
            return ResponseEntity.internalServerError().body(Collections.singletonList("Failed to get History " + e.getMessage()));
        }
        
    }    @GetMapping("/_ah/warmup")
    public String warmup() {
        System.out.println("Warmup request received.");
        return "Warmup done";
    }
}

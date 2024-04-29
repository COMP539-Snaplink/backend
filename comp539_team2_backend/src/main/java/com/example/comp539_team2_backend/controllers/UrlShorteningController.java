package com.example.comp539_team2_backend.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.comp539_team2_backend.JSONResult;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.view.RedirectView;

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

    @PostMapping("/getSubscription")
    public ResponseEntity<JSONResult<Boolean>> getSubscription(@RequestBody UrlRequestDTO request) {
        try {
            String email = request.getEmail();
            return ResponseEntity.ok(new JSONResult<>("success", urlShorteningService.isPremiumUser(email)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONResult<>("error", false));
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
    public ResponseEntity<JSONResult<String>> resolveUrl(@PathVariable("short_url") String shortUrl)  {
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
    public ResponseEntity<JSONResult<String>> customizedUrl(@RequestBody UrlRequestDTO request)  {
        try {
            String key = request.getEmail() == null ? "NO_USER" : request.getEmail();
            String shortened_url = urlShorteningService.customized_url(request.getLongUrl(), request.getShortUrl(), key);
            return ResponseEntity.ok(new JSONResult<>("success", shortened_url));
        } catch (Exception e) {
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return ResponseEntity.internalServerError().body(new JSONResult<>("error", "Failed to customized url: " + errorMessage));
        }
    }

    @PutMapping("/markSpam")
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
    @PutMapping("/removeSpam")
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
        
    }
    @PostMapping("/getTokens")
    public ResponseEntity<String> getTokens(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            String Tokens;
            String email = request.getEmail();
            Tokens= userInfoService.getTokens(email);
            return ResponseEntity.ok(Tokens);
        } 
        catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get Tokens: " + e.getCause().getMessage());
        }
        
    }

    @PostMapping("/resetTokens")
    public ResponseEntity<String> resetTokens(@RequestBody UrlRequestDTO request) throws IOException {
        try {
            Boolean done;
            String email = request.getEmail();
            done= userInfoService.resetTokens(email);
            if (done){
                return ResponseEntity.ok("Reset Tokens successfully");
            }
            else{
                return ResponseEntity.ok("Could not reset Tokens");
            }
            
        } 
        catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to get Tokens: " + e.getCause().getMessage());
        }
        
    }

    @PostMapping("/cleanDB")
    public ResponseEntity<String> cleanDB() {
        try {
            return ResponseEntity.ok(urlShorteningService.cleanData());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to clean the database due to " + e.getCause().getMessage());
        }
    }


}

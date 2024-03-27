package com.example.comp539_team2_backend;

import com.fasterxml.jackson.annotation.JsonProperty;


// DTO class to represent the request payload
public class UrlRequestDTO {
    @JsonProperty("long_url")
    private String longUrl;

    @JsonProperty("short_url")
    private String shortUrl;

    @JsonProperty("email")
    private String email;

    @JsonProperty("long_urls")
    private String[] longUrls;

    @JsonProperty("short_urls")
    private String[] shortUrls;


    // Standard getters and setters
    public String getLongUrl() {
        return longUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public String getEmail() { return email; }

    public String[] getLongUrls() {
        return longUrls;
    }

    public String[] getShortUrls() {
        return shortUrls;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public void setLongUrls(String[] longUrls) {  // Added setter for longUrls
        this.longUrls = longUrls;
    }
}

package com.example.comp539_team2_backend;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// DTO class to represent the request payload
public class UrlRequestDTO {
    @JsonProperty("long_url")
    private String long_url;

    @JsonProperty("long_urls")
    private String[] long_urls;

    // Standard getters and setters
    public String getLongUrl() {
        return long_url;
    }

    public String[] getLongUrls() {
        return long_urls;
    }

    public void setLongUrl(String long_url) {
        this.long_url = long_url;
    }
}

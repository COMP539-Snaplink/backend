package com.example.comp539_team2_backend;

import com.fasterxml.jackson.annotation.JsonProperty;

// DTO class to represent the request payload
public class UrlRequestDTO {
    @JsonProperty("long_url")
    private String long_url;

    // Standard getters and setters
    public String getLongUrl() {
        return long_url;
    }

    public void setLongUrl(String long_url) {
        this.long_url = long_url;
    }
}

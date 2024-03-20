package com.example.comp539_team2_backend.controllers;

import com.example.comp539_team2_backend.entities.GoogleLoginResponse;
import com.example.comp539_team2_backend.entities.GoogleOAuthRequest;
import com.example.comp539_team2_backend.entities.UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SocialLoginController {

    @Value("${google.auth.url}")
    private String googleAuthUrl;

    @Value("${google.login.url}")
    private String googleLoginUrl;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.redirect.url}")
    private String googleRedirectUrl;

    @Value("${google.secret}")
    private String googleClientSecret;

    @GetMapping(value = "/login/getGoogleAuthUrl")
    public ResponseEntity<?> getGoogleAuthUrl(HttpServletRequest request) throws Exception {

        String reqUrl = googleAuthUrl + "?" +
                "client_id=" + googleClientId +
                "&scope=email%20profile%20openid&" +
                "access_type=offline&include_granted_scopes=true&" +
                "response_type=code&" +
                "redirect_uri="+googleRedirectUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(reqUrl));

        //1.login page appears and redirects to / after login success
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }

    // 구글에서 리다이렉션
    @GetMapping(value = "/")
    public UserInfo oauth_google_check(HttpServletRequest request,
                                     @RequestParam(value = "code") String authCode,
                                     HttpServletResponse response) throws Exception{

        GoogleOAuthRequest googleOAuthRequest = GoogleOAuthRequest.builder()
                                                                .clientId(googleClientId)
                                                                .clientSecret(googleClientSecret)
                                                                .code(authCode)
                                                                .redirectUri(googleRedirectUrl)
                                                                .grantType("authorization_code")
                                                                .build();

        RestTemplate restTemplate = new RestTemplate();

        //3.
        ResponseEntity<GoogleLoginResponse> apiResponse = restTemplate.postForEntity("https://oauth2.googleapis.com/token", googleOAuthRequest, GoogleLoginResponse.class);

        // 4. save token as a class
        GoogleLoginResponse googleLoginResponse = apiResponse.getBody();

        String googleToken = googleLoginResponse.getId_token();

        // 5. use google token to retrieve user info
        String requestUrl = "https://oauth2.googleapis.com/tokeninfo?id_token="+googleToken;

        ResponseEntity<String> rest_response = restTemplate.getForEntity(requestUrl, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rest_response.getBody());

        return UserInfo
                .builder()
                .name(root.path("name").asText())
                .email(root.path("email").asText())
                .googleToken(googleToken)
                .code(authCode)
                .subscription(0)
                .build();
    }
}

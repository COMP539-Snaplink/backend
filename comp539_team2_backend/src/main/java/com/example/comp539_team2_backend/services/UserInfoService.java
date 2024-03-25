package com.example.comp539_team2_backend.services;

import com.example.comp539_team2_backend.configs.BigtableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class UserInfoService {
    @Autowired
    BigtableRepository userTableRepository;


    public String getSubscription(String email) throws IOException {
        String subscriptionStatus = "0";
        if (email != null) {
            subscriptionStatus = userTableRepository.get(email, "user", "subscription");
        }
        return subscriptionStatus;
    }
}

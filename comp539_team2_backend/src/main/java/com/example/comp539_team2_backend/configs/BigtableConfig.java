package com.example.comp539_team2_backend.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigtableConfig {

    @Value("${bigtable.projectId}")
    private String projectId;

    @Value("${bigtable.instanceId}")
    private String instanceId;

    @Bean
    public BigtableRepository urlTableRepository() {
        return new BigtableRepository(projectId, instanceId, "spring24-team2-snaplink");
    }

    @Bean
    public BigtableRepository userTableRepository() {
        return new BigtableRepository(projectId, instanceId, "spring24-team2-snaplink-userTable");
    }

}

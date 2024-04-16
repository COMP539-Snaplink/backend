package com.example.comp539_team2_backend.configs;

import com.example.comp539_team2_backend.configs.BigtableRepository;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class BigtableConfig {

    @Value("${bigtable.projectId}")
    private String projectId;

    @Value("${bigtable.instanceId}")
    private String instanceId;

    @Value("${bigtable.tableId}")
    private String tableId;

    @Bean
    public BigtableDataClient bigtableDataClient() throws IOException {
        BigtableDataSettings settings = BigtableDataSettings.newBuilder()
                .setProjectId(projectId)
                .setInstanceId(instanceId)
                .build();
        return BigtableDataClient.create(settings);
    }

    @Bean
    public BigtableRepository bigtableRepository(BigtableDataClient bigtableDataClient) {
        return new BigtableRepository(bigtableDataClient, tableId);
    }
    // @Bean
    // public BigtableRepository urlTableRepository() {
    //     return new BigtableRepository(projectId, instanceId, "spring24-team2-snaplink");
    // }
}

package com.example.comp539_team2_backend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hbase.thirdparty.io.netty.handler.codec.mqtt.MqttMessageBuilders;


@Data
@Builder
public class UserInfo {
    private String name;
    private String googleToken;
    private String code;
    private Integer subscription;
    private Integer tokens;

    @JsonProperty("email")
    private String email;

    public String getEmail() {
        return email;
    }
}

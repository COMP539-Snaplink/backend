package com.example.comp539_team2_backend.entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hbase.thirdparty.io.netty.handler.codec.mqtt.MqttMessageBuilders;

@Data
@Builder
public class UserInfo {
    private String name;   // 반환된 토큰 유형(Bearer 고정)
    private String email;
    private String googleToken;

}

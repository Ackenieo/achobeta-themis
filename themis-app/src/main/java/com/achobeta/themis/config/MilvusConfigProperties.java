package com.achobeta.themis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "milvus", ignoreInvalidFields = true)
public class MilvusConfigProperties {
    private String host;
    private int port;
    private String token;
    private int connectTimeout;
    private int keepAliveTime;
    private String databaseName;
}

package com.achobeta.themis.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "meilisearch", ignoreInvalidFields = true)
public class MeiliSearchConfigProperties {

    private String host;
    @JsonProperty("api-key")
    private String apiKey;

}

package com.achobeta.themis.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(MeiliSearchConfigProperties.class)
public class MeiliSearchConfig {

    @Autowired
    private MeiliSearchConfigProperties meiliSearchConfigProperties;

    @Bean
    public Client meiliSearchClient() {
        Config config = new Config(meiliSearchConfigProperties.getHost(), meiliSearchConfigProperties.getApiKey());
        return new Client(config);
    }

}

package com.achobeta.themis.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(MilvusConfigProperties.class)
public class MilvusConfig {
    @Autowired
    private MilvusConfigProperties milvusConfigProperties;

    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam.Builder connectParamBuilder = ConnectParam.newBuilder()
                .withHost(milvusConfigProperties.getHost())
                .withPort(milvusConfigProperties.getPort())
                .withConnectTimeout(milvusConfigProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .withKeepAliveTime(milvusConfigProperties.getKeepAliveTime(), TimeUnit.MILLISECONDS)
                .withDatabaseName(milvusConfigProperties.getDatabaseName());

        if (milvusConfigProperties.getToken() != null && !milvusConfigProperties.getToken().isEmpty()) {
            connectParamBuilder.withToken(milvusConfigProperties.getToken());
        }
        return new MilvusServiceClient(connectParamBuilder.build());
    }
}

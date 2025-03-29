package com.sdumagicode.backend.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host; //milvus所在服务器地址
    @Value("${milvus.port}")
    private Integer port; //milvus端口
    @Value("${milvus.username}")
    private String username;

    @Value("${milvus.password}")
    private String password;

    @Value("${milvus.database:default}")
    private String database;

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withAuthorization(username, password)  // 设置账号密码
                .withDatabaseName(database)  // 设置数据库
                .build();
        return new MilvusServiceClient(connectParam);
    }
}

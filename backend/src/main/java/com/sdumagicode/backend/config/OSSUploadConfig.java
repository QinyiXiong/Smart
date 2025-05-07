package com.sdumagicode.backend.config;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class OSSUploadConfig {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String maxSize;
    private String prefix;

    public OSSUploadConfig() {
        Dotenv dotenv = Dotenv.configure().load();
        this.endpoint = dotenv.get("ALIYUN_OSS_ENDPOINT");
        this.accessKeyId = dotenv.get("ALIYUN_OSS_ACCESS_KEY_ID");
        this.accessKeySecret = dotenv.get("ALIYUN_OSS_ACCESS_KEY_SECRET");
        this.bucketName = dotenv.get("ALIYUN_OSS_BUCKET_NAME");
        this.maxSize = dotenv.get("ALIYUN_OSS_MAX_SIZE");
        this.prefix = dotenv.get("ALIYUN_OSS_DIR_PREFIX");
    }

    @Bean
    public OSSClient createOssClient() {
        return (OSSClient) new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
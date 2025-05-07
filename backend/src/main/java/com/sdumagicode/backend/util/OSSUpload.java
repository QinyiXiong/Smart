package com.sdumagicode.backend.util;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectRequest;
import com.sdumagicode.backend.config.OSSUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

@Service
public class OSSUpload {
    private final OSSClient ossClient;
    private final OSSUploadConfig ossConfig;

    @Autowired
    public OSSUpload(OSSClient ossClient, OSSUploadConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    public String uploadBase64ToOSS(String base64Data, String folder) {
        try {
            // 1. 检查 Base64 数据
            if (base64Data == null || !base64Data.startsWith("data:image/")) {
                throw new IllegalArgumentException("Base64 数据格式不正确");
            }

            // 2. 解析 Base64
            String[] parts = base64Data.split(",");
            String header = parts[0]; // data:image/png;base64
            String data = parts[1];   // 实际数据

            // 3. 获取文件扩展名
            String extension = header.split(";")[0].split("/")[1];

            // 4. 生成唯一文件名
            String fileName = folder + UUID.randomUUID() + "." + extension;

            // 5. 解码并上传
            byte[] bytes = Base64.getDecoder().decode(data);
            ossClient.putObject(
                    ossConfig.getBucketName(),
                    fileName,
                    new ByteArrayInputStream(bytes)
            );

            // 6. 返回访问 URL
            return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("上传图片到 OSS 失败: " + e.getMessage(), e);
        }
    }
}
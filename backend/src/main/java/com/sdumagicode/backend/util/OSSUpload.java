package com.sdumagicode.backend.util;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectRequest;
import com.sdumagicode.backend.config.OSSUploadConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    
    /**
     * 上传MultipartFile文件到OSS
     * 
     * @param file MultipartFile文件
     * @param folder 文件夹路径
     * @return 访问URL
     * @throws IOException IO异常
     */
    public String uploadFileToOSS(MultipartFile file, String folder) throws IOException {
        try {
            // 1. 获取文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String fileType = originalFilename.substring(originalFilename.lastIndexOf("."));
            
            // 2. 生成唯一文件名
            String fileName = folder + UUID.randomUUID() + fileType;
            
            // 3. 上传文件到OSS
            ossClient.putObject(
                    ossConfig.getBucketName(),
                    fileName,
                    new ByteArrayInputStream(file.getBytes())
            );
            
            // 4. 返回访问URL
            return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("上传文件到OSS失败: " + e.getMessage(), e);
        }
    }
}


package com.sdumagicode.backend.util;

import com.sdumagicode.backend.entity.chat.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class PhotoUploadUtil {


    private static String uploadDir = new File("").getAbsolutePath() + "/src/main/resources/static/photos/";

    /**
     * 上传文件并返回文件信息
     * @param file 上传的文件
     * @return FileInfo 包含文件信息的对象
     * @throws IOException 文件操作异常
     */
    public static FileInfo uploadFile(MultipartFile file) throws IOException {
        // 确保上传目录存在
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // 保存文件
        Path filePath = uploadPath.resolve(uniqueFileName);
        file.transferTo(filePath.toFile());

        // 构建文件信息对象
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(UUID.randomUUID().toString());
        fileInfo.setFileName(originalFilename);
        fileInfo.setType(getFileType(fileExtension));
        fileInfo.setSize(file.getSize());
        fileInfo.setUrl("/uploads/" + uniqueFileName); // 可以根据实际需求调整URL

        return fileInfo;
    }

    private static String getFileType(String fileExtension) {
        fileExtension = fileExtension.toLowerCase();
        if (fileExtension.endsWith(".png")) {
            return "png";
        } else if (fileExtension.endsWith(".jpg")){
            return "jpg";
        } else {
            return "other";
        }
    }

    /**
     * 根据文件URL删除上传的文件
     * @param fileUrl 文件的URL路径（如：/uploads/filename.ext）
     * @return boolean 是否删除成功
     * @throws IOException 文件操作异常
     */
    public static boolean deleteFileByUrl(String fileUrl) throws IOException {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }

        // 从URL中提取文件名
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);

        // 构建文件完整路径
        Path filePath = Paths.get(uploadDir, fileName);

        // 检查文件是否存在并删除
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        }

        return false;
    }

}

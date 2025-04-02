package com.sdumagicode.backend.util.chatUtil;

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
public class FileUploadUtil {


    private static String uploadDir = new File("").getAbsolutePath() + "/src/main/resources/static/uploads/";

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

    /**
     * 根据文件扩展名获取文件类型
     * @param fileExtension 文件扩展名
     * @return 文件类型
     */
    private static String getFileType(String fileExtension) {
        fileExtension = fileExtension.toLowerCase();
        if (fileExtension.endsWith(".pdf")) {
            return "pdf";
        } else if (fileExtension.endsWith(".doc") || fileExtension.endsWith(".docx")) {
            return "word";
        } else if (fileExtension.endsWith(".mp3")) {
            return "mp3";
        } else if (fileExtension.endsWith(".wav")) {
            return "wav";
        } else if (fileExtension.endsWith(".mp4")) {
            return "mp4";
        } else {
            return "other";
        }
    }
}

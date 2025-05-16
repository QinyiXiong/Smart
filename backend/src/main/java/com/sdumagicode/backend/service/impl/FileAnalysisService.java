package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.entity.chat.Content;

import com.sdumagicode.backend.entity.chat.FileInfo;
import com.sdumagicode.backend.util.chatUtil.AudioAnalysisUtil;
import com.sdumagicode.backend.util.chatUtil.FileContentExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Service
public class FileAnalysisService {

    @Autowired
    private AudioAnalysisUtil audioAnalysisUtil;

    private static final String UPLOAD_DIR = new File("").getAbsolutePath() + "/src/main/resources/static/";

    public MessageFileDto analyzeFileInfo(FileInfo fileInfo){
        if(fileInfo.getType() == "mp3"){
            return analyzeAudio(fileInfo);
        }else{
            return  analyzeFile(fileInfo);
        }
    }


    /**
     * 分析并整合所有内容（text内容放在最后）
     * @param
     * @return 整合后的内容对象
     */
    public MessageFileDto analyzeAudio(FileInfo fileInfo) {
        StringBuilder combinedText = new StringBuilder();
        
        // 1. 处理语音内容
        if (fileInfo != null && fileInfo.getUrl() != null) {
            try {
                String fullPath = new File(UPLOAD_DIR, fileInfo.getUrl()).getAbsolutePath();
                String audioAnalysis = audioAnalysisUtil.analyzeInterviewAudio(fullPath);
                combinedText.append(audioAnalysis).append("\n\n");
            } catch (Exception e) {
                e.printStackTrace();
                combinedText.append("<语音分析失败>\n\n");
            }
        }
        MessageFileDto messageFileDto = new MessageFileDto(fileInfo);
        messageFileDto.setText(combinedText.toString().trim());
        return messageFileDto;
    }

    public MessageFileDto analyzeFile(FileInfo fileInfo){

        StringBuilder combinedText = new StringBuilder();

        if (fileInfo != null && fileInfo.getUrl() != null) {

                String fileContent = processFile(fileInfo);
                if (fileContent != null) {
                    combinedText.append("<文件内容-").append(fileInfo.getFileName()).append(">\n")
                            .append(fileContent)
                            .append("\n</文件内容-").append(fileInfo.getFileName()).append(">\n\n");
                }

        }

        MessageFileDto messageFileDto = new MessageFileDto(fileInfo);
        messageFileDto.setText(combinedText.toString().trim());
        return messageFileDto;
    }

    private String processFile(FileInfo fileInfo) {
        try (InputStream inputStream = getInputStreamFromFileInfo(fileInfo)) {
            String suffix = getFileSuffix(fileInfo.getFileName());
            return FileContentExtractor.extractContent(suffix, inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceException("文件上传异常");
        }
    }

    /**
     * 根据文件url获取文件输入流
     * @param fileInfo
     * @return
     * @throws IOException
     */
    private InputStream getInputStreamFromFileInfo(FileInfo fileInfo) throws IOException {
        // 实现根据实际情况调整
        return new FileInputStream(new File(UPLOAD_DIR, fileInfo.getUrl()));
    }

    private String getFileSuffix(String fileName) {
        if (fileName == null) return null;
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : null;
    }
}

package com.sdumagicode.backend.dto.chat;

import com.sdumagicode.backend.entity.chat.FileInfo;
import lombok.Data;

@Data
public class MessageFileDto {
    private FileInfo fileInfo;  // 使用组合代替继承
    private String text;
    public MessageFileDto(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
    // 提供访问方法
    public FileInfo getFileInfo() {
        return fileInfo;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}
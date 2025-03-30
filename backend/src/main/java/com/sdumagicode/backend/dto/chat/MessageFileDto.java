package com.sdumagicode.backend.dto.chat;

import com.sdumagicode.backend.entity.chat.FileInfo;
import lombok.Data;

@Data
public class MessageFileDto extends FileInfo {
    public MessageFileDto(FileInfo fileInfo){
        setFileName(fileInfo.getFileName());
        setFileId(fileInfo.getFileId());
        setUrl(fileInfo.getUrl());
        setType(fileInfo.getType());
        setSize(fileInfo.getSize());
    }


    private String text;
}

package com.sdumagicode.backend.entity.chat;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String fileId;
    private String fileName;
    private String type; // 'pdf', 'word', 'mp3', 'wav', 'mp4' etc.
    //存储文件的文本内容
    private String textContent;
    private long size;
    private String url;

}

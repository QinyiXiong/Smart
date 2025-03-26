package com.sdumagicode.backend.entity.chat;


import lombok.Data;

@Data
public class FileInfo {
    private String fileId;
    private String type; // 'pdf', 'word', 'mp3', 'wav', 'mp4' etc.
    private long size;
    private String url;

}

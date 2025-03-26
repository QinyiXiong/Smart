package com.sdumagicode.backend.entity.chat;

import lombok.Data;
import org.apache.tomcat.jni.FileInfo;

import java.util.List;

@Data
public class Content {

    private String text;
    private Voice voice;
    private List<FileInfo> files;
}

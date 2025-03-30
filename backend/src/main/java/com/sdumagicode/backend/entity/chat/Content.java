package com.sdumagicode.backend.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    private String text;
    private Voice voice;
    private List<FileInfo> files;
}

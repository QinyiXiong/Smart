package com.sdumagicode.backend.entity.chat;


import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("interviewer")
public class Interviewer {

    @Id
    private String interviewerId;

    private String name;

    private Long userId;

    private String knowledgeBaseId;


    private String customPrompt;

    private List<AiSettingContent> settingsList;
}

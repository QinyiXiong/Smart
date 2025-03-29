package com.sdumagicode.backend.entity.chat;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Interviewer {

    private Long interviewerId;

    private Long userId;

    //是否使用系统默认题目库（如果指定了数据库Id，则使用用户自定义的知识库）
    private String dataBaseId;

    //该面试官使用的提示词模板，包含变量文本
    private String promptTemplate;

    private List<Map.Entry<AiSettings,Integer>> settingsList;
}

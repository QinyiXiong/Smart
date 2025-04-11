package com.sdumagicode.backend.dto;

import lombok.Data;

/**
 * 代码运行请求DTO
 */
@Data
public class CodeRunDTO {
    /**
     * 题目ID
     */
    private Long problemId;
    
    /**
     * 提交的代码
     */
    private String code;
    
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 自定义输入
     */
    private String input;
}
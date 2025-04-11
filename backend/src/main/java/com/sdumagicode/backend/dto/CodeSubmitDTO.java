package com.sdumagicode.backend.dto;

import lombok.Data;

/**
 * 代码提交请求DTO
 */
@Data
public class CodeSubmitDTO {
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
}
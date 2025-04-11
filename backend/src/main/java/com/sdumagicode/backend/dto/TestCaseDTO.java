package com.sdumagicode.backend.dto;

import lombok.Data;

/**
 * 测试用例DTO
 */
@Data
public class TestCaseDTO {
    /**
     * 测试用例输入
     */
    private String input;
    
    /**
     * 预期输出
     */
    private String output;
    
    /**
     * 测试点分值（可选）
     */
    private Integer score;
    
    /**
     * 是否为示例测试用例
     */
    private Boolean isExample;
}
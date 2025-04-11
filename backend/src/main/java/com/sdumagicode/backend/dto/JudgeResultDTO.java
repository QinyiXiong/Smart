package com.sdumagicode.backend.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 代码评测结果DTO
 */
@Data
@Builder
public class JudgeResultDTO {
    /**
     * 评测状态：success, error, timeout, memory_exceeded, compile_error, runtime_error, wrong_answer
     */
    private String status;
    
    /**
     * 执行时间(ms)
     */
    private Integer time;
    
    /**
     * 内存消耗(MB)
     */
    private Integer memory;
    
    /**
     * 输出结果
     */
    private String output;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 通过的测试用例数
     */
    private Integer passedTestCases;
    
    /**
     * 测试用例总数
     */
    private Integer totalTestCases;
    
    /**
     * 提交ID
     */
    private Long submissionId;
}
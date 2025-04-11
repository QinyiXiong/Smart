package com.sdumagicode.backend.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 代码提交实体类
 */
@Data
@Table(name = "oj_code_submission")
public class CodeSubmission implements Serializable {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 题目ID
     */
    @Column(nullable = false)
    private Long problemId;

    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 提交的代码
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    /**
     * 编程语言
     */
    @Column(nullable = false)
    private String language;

    /**
     * 执行状态：pending, running, accepted, wrong_answer, time_limit_exceeded, memory_limit_exceeded, compile_error, runtime_error
     */
    @Column(nullable = false)
    private String status;

    /**
     * 执行时间(ms)
     */
    private Integer executionTime;

    /**
     * 内存消耗(MB)
     */
    private Integer memoryUsage;

    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 测试点通过数
     */
    private Integer passedTestCases;

    /**
     * 测试点总数
     */
    private Integer totalTestCases;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        this.submittedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "pending";
        }
    }
}
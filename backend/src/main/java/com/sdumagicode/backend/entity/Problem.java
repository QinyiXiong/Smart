package com.sdumagicode.backend.entity;

import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Table(name = "oj_problem")
public class Problem implements Serializable, Cloneable {
    @Id
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 题目编号（例如：P1001）
     */
    @Column(unique = true)
    private String problemCode;

    /**
     * 题目名称
     */
    @Column(nullable = false)
    private String title;

    /**
     * 题目描述（支持Markdown格式）
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 输入描述
     */
    @Column(columnDefinition = "TEXT")
    private String inputDescription;

    /**
     * 输出描述
     */
    @Column(columnDefinition = "TEXT")
    private String outputDescription;

    /**
     * 题目难度（简单、中等、困难）
     */
    @Column(nullable = false)
    private String difficulty;

    /**
     * 时间限制（单位：ms）
     */
    private Integer timeLimit;

    /**
     * 内存限制（单位：MB）
     */
    private Integer memoryLimit;

    /**
     * 题目标签/分类（JSON数组格式）
     */
    @Column(columnDefinition = "TEXT")
    private List<String> tags;

    /**
     * 示例输入
     */
    @Column(columnDefinition = "TEXT")
    private String sampleInput;

    /**
     * 示例输出
     */
    @Column(columnDefinition = "TEXT")
    private String sampleOutput;

    /**
     * 测试用例（JSON格式存储，包含输入、预期输出、分值等信息）
     * 格式：[
     *   {"input": "测试输入", "output": "期望输出", "score": 10},
     *   ...
     * ]
     * 其中：
     * - input: 测试输入数据
     * - output: 期望的输出结果
     * - score: 该测试点的分值，默认为0
     */
    @Column(columnDefinition = "TEXT")
    private String testCases;

    /**
     * 提示/提示（可选）
     */
    @Column(columnDefinition = "TEXT")
    private String hints;

    /**
     * 题目来源
     */
    private String source;

    /**
     * 解题人数
     */
    private Integer submitCount;

    /**
     * 通过人数
     */
    private Integer acceptCount;

    /**
     * 通过率
     */
    @Column(precision = 4, scale = 2)
    private Double acceptanceRate;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 是否可见（0-否，1-是）
     */
    private Integer visible;

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        if (this.visible == null) {
            this.visible = 1;
        }
        if (this.submitCount == null) {
            this.submitCount = 0;
        }
        if (this.acceptCount == null) {
            this.acceptCount = 0;
        }
        if (this.acceptanceRate == null) {
            this.acceptanceRate = 0.0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
        if (this.submitCount > 0) {
            this.acceptanceRate = (double) this.acceptCount / this.submitCount;
        }
    }
}
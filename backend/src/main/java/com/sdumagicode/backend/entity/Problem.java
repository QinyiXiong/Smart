package com.sdumagicode.backend.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Table(name = "oj_problem")
public class Problem implements Serializable, Cloneable {
    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "id")
    private Long id;

    /**
     * 题目名称
     */
    private String title;

    /**
     * 题目描述
     */
    private String description;

    /**
     * 题目难度
     */
    private String difficulty;

    /**
     * 题目通过率
     */
    private String acceptance;

    /**
     * 测试用例
     */
    private String testCases;

    /**
     * 题目分类
     */
    private String category;
}
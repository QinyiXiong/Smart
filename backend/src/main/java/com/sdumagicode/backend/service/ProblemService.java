package com.sdumagicode.backend.service;

import com.sdumagicode.backend.dto.ProblemDTO;

import java.util.List;
import java.util.Map;

public interface ProblemService {
    ProblemDTO getProblemById(Long id);
    void addProblem(ProblemDTO problemDTO);
    void updateProblem(ProblemDTO problemDTO);
    void deleteProblem(Long id);
    
    /**
     * 查询题目列表（支持分页）
     * @param difficulty 难度
     * @param category 分类
     * @param page 页码
     * @param size 每页大小
     * @return 题目列表
     */
    List<ProblemDTO> selectProblemsWithPagination(String difficulty, String category, int page, int size);
    
    /**
     * 查询题目列表（不分页）
     * @param difficulty 难度
     * @param category 分类
     * @return 题目列表
     */
    List<ProblemDTO> selectProblems(String difficulty, String category);
    
    /**
     * 根据分类查询题目列表（支持分页）
     * @param category 分类
     * @param page 页码
     * @param size 每页大小
     * @return 题目列表
     */
    List<ProblemDTO> selectProblemsByCategoryWithPagination(String category, int page, int size);
    
    /**
     * 根据分类查询题目列表（不分页）
     * @param category 分类
     * @return 题目列表
     */
    List<ProblemDTO> selectProblemsByCategory(String category);
    
    /**
     * 根据难度查询题目列表（支持分页）
     * @param difficulty 难度
     * @param page 页码
     * @param size 每页大小
     * @return 题目列表
     */
    List<ProblemDTO> selectProblemsByDifficultyWithPagination(String difficulty, int page, int size);
    
    /**
     * 根据难度查询题目列表（不分页）
     * @param difficulty 难度
     * @return 题目列表
     */
    List<ProblemDTO> selectProblemsByDifficulty(String difficulty);
    
    /**
     * 获取题目总数
     * @param difficulty 难度
     * @param category 分类
     * @return 题目总数
     */
    int countProblems(String difficulty, String category);
    
    Long postProblem(ProblemDTO problemDTO);
    
    Integer delete(Long id);
    
    ProblemDTO selectProblemById(Long id);

    List<String> getAllTags();
    
    Map<String, Object> getStatistics();
    
    /**
     * 刷新缓存
     */
    void refreshCache();
}
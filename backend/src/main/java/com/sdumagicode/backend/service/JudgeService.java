package com.sdumagicode.backend.service;

import com.sdumagicode.backend.dto.CodeRunDTO;
import com.sdumagicode.backend.dto.CodeSubmitDTO;
import com.sdumagicode.backend.dto.JudgeResultDTO;
import com.sdumagicode.backend.entity.CodeSubmission;

/**
 * 代码评测服务接口
 */
public interface JudgeService {
    
    /**
     * 运行代码（自定义输入）
     * @param codeRunDTO 代码运行请求
     * @return 运行结果
     */
    JudgeResultDTO runCode(CodeRunDTO codeRunDTO);
    
    /**
     * 提交代码（使用题目测试用例进行评测）
     * @param codeSubmitDTO 代码提交请求
     * @return 评测结果
     */
    JudgeResultDTO submitCode(CodeSubmitDTO codeSubmitDTO);
    
    /**
     * 获取代码提交记录
     * @param problemId 题目ID
     * @param userId 用户ID
     * @return 提交记录列表
     */
    java.util.List<CodeSubmission> getSubmissionHistory(Long problemId, Long userId);
}
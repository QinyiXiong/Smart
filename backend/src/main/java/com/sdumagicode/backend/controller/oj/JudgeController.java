package com.sdumagicode.backend.controller.oj;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.CodeRunDTO;
import com.sdumagicode.backend.dto.CodeSubmitDTO;
import com.sdumagicode.backend.dto.JudgeResultDTO;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.mapper.CodeSubmissionMapper;
import com.sdumagicode.backend.service.JudgeService;
import com.sdumagicode.backend.util.UserUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代码评测控制器
 */
@RestController
@RequestMapping("/api/v1/problems")
public class JudgeController {

    @Autowired
    private JudgeService judgeService;
    
    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;  // 添加这一行

    /**
     * 运行代码（自定义输入）
     * @param codeRunDTO 代码运行请求
     * @return 运行结果
     */
    @PostMapping("/run")
    @RequiresPermissions(value = "user")
    public GlobalResult<JudgeResultDTO> runCode(@RequestBody CodeRunDTO codeRunDTO) {
        JudgeResultDTO result = judgeService.runCode(codeRunDTO);
        return GlobalResultGenerator.genSuccessResult(result);
    }

    /**
     * 提交代码（使用题目测试用例进行评测）
     * @param codeSubmitDTO 代码提交请求
     * @return 评测结果
     */
    @PostMapping("/submit")
    @RequiresPermissions(value = "user")
    public GlobalResult<JudgeResultDTO> submitCode(@RequestBody CodeSubmitDTO codeSubmitDTO) {
        JudgeResultDTO result = judgeService.submitCode(codeSubmitDTO);
        return GlobalResultGenerator.genSuccessResult(result);
    }

    /**
     * 获取提交历史
     * @param problemId 题目ID
     * @param page 页码
     * @param rows 每页行数
     * @return 提交历史列表
     */
    @GetMapping("/{problemId}/submissions")
    @RequiresPermissions(value = "user")
    public GlobalResult<PageInfo<CodeSubmission>> getSubmissions(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer rows) {
        User currentUser = UserUtils.getCurrentUserByToken();
        PageHelper.startPage(page, rows);
        List<CodeSubmission> list = judgeService.getSubmissionHistory(problemId, currentUser.getIdUser());
        PageInfo<CodeSubmission> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
    }

    /**
     * 获取评测结果
     */
    @GetMapping("/submissions/{submissionId}")
    @RequiresPermissions(value = "user")
    public GlobalResult<JudgeResultDTO> getSubmissionResult(@PathVariable Long submissionId) {
        CodeSubmission submission = codeSubmissionMapper.selectByPrimaryKey(submissionId);
        
        JudgeResultDTO result = JudgeResultDTO.builder()
                .status(submission.getStatus())
                .time(submission.getExecutionTime())
                .memory(submission.getMemoryUsage())
                .errorMessage(submission.getErrorMessage())
                .passedTestCases(submission.getPassedTestCases())
                .totalTestCases(submission.getTotalTestCases())
                .build();
                
        return GlobalResultGenerator.genSuccessResult(result);
    }
}
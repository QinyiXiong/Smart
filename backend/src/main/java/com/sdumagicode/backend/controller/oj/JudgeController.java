package com.sdumagicode.backend.controller.oj;



import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.*;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.service.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.CodeRunDTO;
import com.sdumagicode.backend.dto.CodeSubmitDTO;
import com.sdumagicode.backend.dto.JudgeResultDTO;
import com.sdumagicode.backend.dto.chat.ChatOutput;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.mapper.CodeSubmissionMapper;
import com.sdumagicode.backend.service.JudgeService;
import com.sdumagicode.backend.util.UserUtils;
import com.sdumagicode.backend.util.chatUtil.MessageQueueUtil;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
        
        // 如果评测成功且有提交ID，自动发送代码给AI进行评价
        if (result != null && result.getSubmissionId() != null) {
            // 创建代码提交对象，只设置ID
            CodeSubmission submission = new CodeSubmission();
            submission.setId(result.getSubmissionId());
            submission.setSubmittedAt(LocalDateTime.now()); // 显式设置提交时间为当前时间
                
            // 调用服务获取AI评价
            judgeService.getAiCodeReview(submission);
            // 设置标志，表示已经发送给AI评价
            result.setAiReviewRequested(true);
        }
        
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
    
    /**
     * 获取AI对代码的评价
     * @param submissionId 提交ID
     * @return 消息ID，用于后续轮询获取评价结果
     */
    @PostMapping("/submissions/{submissionId}/ai-review")
    @RequiresPermissions(value = "user")
    public GlobalResult<String> getAiCodeReview(@PathVariable Long submissionId) {
        // 创建代码提交对象，只设置ID
        CodeSubmission submission = new CodeSubmission();
        submission.setId(submissionId);
        
        try {
            // 调用服务获取AI评价
            String messageId = judgeService.getAiCodeReview(submission);
            return GlobalResultGenerator.genSuccessStringDataResult(messageId);
        } catch (Exception e) {
            // 返回错误信息
            return GlobalResultGenerator.genErrorResult("获取AI代码评价失败: " + e.getMessage());
        }
    }
    
    /**
     * 轮询获取AI评价结果
     * @param messageId 消息ID
     * @param batchSize 批次大小
     * @return AI评价结果列表
     */
    @GetMapping("/poll-ai-review")
    @RequiresPermissions(value = "user")
    public GlobalResult<List<ChatOutput>> pollAiReview(
            @RequestParam("messageId") String messageId,
            @RequestParam(defaultValue = "10") int batchSize) {
        
        // 参数校验
        if (messageId == null || messageId.isEmpty()) {
            return GlobalResultGenerator.genErrorResult("messageId不能为空");
        }
        if (batchSize <= 0 || batchSize > 100) {
            batchSize = 10; // 设置合理的默认值
        }
        
        // 获取当前用户ID
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        
        // 设置轮询超时参数
        long startTime = System.currentTimeMillis();
        final long timeout = 5000; // 5秒超时
        
        List<ChatOutput> batch = new ArrayList<>(batchSize);
        boolean hasStopSignal = false;
        
        try {
            // 检查是否超时
            if ((System.currentTimeMillis() - startTime) < timeout) {
                // 从消息队列中获取批量消息
                List<ChatOutput> messages = MessageQueueUtil.pollBatch(messageId, batchSize);
                
                if (messages != null && !messages.isEmpty()) {
                    batch.addAll(messages);
                    
                    // 检查是否有停止信号
                    for (ChatOutput msg : messages) {
                        if ("stop".equals(msg.getFinish())) {
                            hasStopSignal = true;
                            break;
                        }
                    }
                }
            }
            
            return GlobalResultGenerator.genSuccessResult(batch);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("获取AI评价失败: " + e.getMessage());
        }
    }
}
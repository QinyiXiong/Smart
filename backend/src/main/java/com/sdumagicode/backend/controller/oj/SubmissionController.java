package com.sdumagicode.backend.controller.oj;

import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.mapper.CodeSubmissionMapper;
import com.sdumagicode.backend.util.UserUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码提交统计控制器
 */
@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;

    /**
     * 获取当前用户的提交热力图数据
     * 返回格式：{ "2023-01-01": 5, "2023-01-02": 3, ... }
     * 其中键是日期（YYYY-MM-DD格式），值是当天的提交次数
     * @return 提交热力图数据
     */
    @GetMapping("/heatmap")
    @RequiresPermissions(value = "user")
    public GlobalResult<Map<String, Integer>> getSubmissionHeatmap() {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUserByToken();
        if (currentUser == null) {
            return GlobalResultGenerator.genErrorResult("用户未登录");
        }
        
        // 查询该用户的所有提交记录
        CodeSubmission example = new CodeSubmission();
        example.setUserId(currentUser.getIdUser());
        List<CodeSubmission> submissions = codeSubmissionMapper.select(example);
        
        // 按日期分组并计数
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Integer> heatmapData = new HashMap<>();
        
        // 处理提交数据，按日期分组计数
        Map<String, Long> submissionCounts = submissions.stream()
                .map(submission -> submission.getSubmittedAt().toLocalDate().format(formatter))
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));
        
        // 转换Long到Integer
        submissionCounts.forEach((date, count) -> heatmapData.put(date, count.intValue()));
        
        return GlobalResultGenerator.genSuccessResult(heatmapData);
    }
}
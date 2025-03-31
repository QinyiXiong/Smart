package com.sdumagicode.backend.controller.chat;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.service.InterviewerService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/Interviewer")
@RequiresPermissions(value = "user")
public class InterviewerController {

    @Autowired
    private InterviewerService interviewerService;

    /**
     * 新增或更新面试官信息
     * @param interviewer 面试官实体
     * @return 操作结果
     */
    @PostMapping("/saveOrUpdate")
    public GlobalResult saveOrUpdateInterviewer(@RequestBody Interviewer interviewer) {
        if (interviewer == null) {
            throw new ServiceException("参数不能为空");
        }
        interviewerService.saveOrUpdateInterviewer(interviewer);
        return GlobalResultGenerator.genSuccessResult(
                );
    }

    /**
     * 删除面试官
     * @param interviewerId 面试官ID
     * @return 操作结果
     */
    @DeleteMapping("/delete/{interviewerId}")
    public GlobalResult deleteInterviewer(@PathVariable String interviewerId) {
        if (interviewerId != null && StringUtils.isBlank(interviewerId)) {
            throw new ServiceException("ID不能为空");
        }
        interviewerService.deleteInterviewer(interviewerId);
        return GlobalResultGenerator.genSuccessResult();
    }

    /**
     * 获取所有面试官列表
     * @return 面试官列表
     */
    @GetMapping("/list")
    public GlobalResult<List<Interviewer>> findInterviewers() {
        return GlobalResultGenerator.genSuccessResult(
                interviewerService.findInterviewers());
    }
}

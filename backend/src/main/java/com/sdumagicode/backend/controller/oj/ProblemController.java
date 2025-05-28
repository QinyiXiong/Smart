package com.sdumagicode.backend.controller.oj;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.exception.BusinessException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.core.service.redis.RedisService;
import com.sdumagicode.backend.core.service.security.annotation.AuthorshipInterceptor;
import com.sdumagicode.backend.dto.ProblemDTO;
import com.sdumagicode.backend.enumerate.Module;
import com.sdumagicode.backend.service.ProblemService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {
    @Autowired
    private ProblemService problemService;
    
    @Autowired
    private RedisService redisService;

    @GetMapping
public GlobalResult<PageInfo<ProblemDTO>> getProblemList(
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "100") Integer rows) {
    // 使用优化后的分页查询方法
    List<ProblemDTO> list = problemService.selectProblemsWithPagination(difficulty, category, page, rows);
    // 获取总数
    int total = problemService.countProblems(difficulty, category);
    // 手动构建PageInfo对象
    PageInfo<ProblemDTO> pageInfo = new PageInfo<>(list);
    pageInfo.setTotal(total);
    pageInfo.setPageNum(page);
    pageInfo.setPageSize(rows);
    return GlobalResultGenerator.genSuccessResult(pageInfo);
}

@GetMapping("/count")
public GlobalResult<Integer> getProblemCount(
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) String category) {
    int count = problemService.countProblems(difficulty, category);
    return GlobalResultGenerator.genSuccessResult(count);
}

@GetMapping("/all")
public GlobalResult<List<ProblemDTO>> getAllProblems(
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) String category) {
    List<ProblemDTO> list = problemService.selectProblems(difficulty, category);
    return GlobalResultGenerator.genSuccessResult(list);
}

    @GetMapping("/tags")
    public GlobalResult<List<String>> getAllTags() {
        return GlobalResultGenerator.genSuccessResult(problemService.getAllTags());
    }

    @GetMapping("/statistics")
    public GlobalResult<Map<String, Object>> getStatistics() {
        return GlobalResultGenerator.genSuccessResult(problemService.getStatistics());
    }

    @GetMapping("/{id}")
    public GlobalResult<ProblemDTO> getProblemById(@PathVariable Long id) {
        if (Objects.isNull(id)) {
            throw new BusinessException("题目ID不能为空!");
        }
        return GlobalResultGenerator.genSuccessResult(problemService.selectProblemById(id));
    }

    @GetMapping("/category/{category}")
public GlobalResult<PageInfo<ProblemDTO>> getProblemsByCategory(
        @PathVariable String category,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "100") Integer rows) {
    // 使用优化后的分页查询方法
    List<ProblemDTO> list = problemService.selectProblemsByCategoryWithPagination(category, page, rows);
    // 获取总数
    int total = problemService.countProblems(null, category);
    // 手动构建PageInfo对象
    PageInfo<ProblemDTO> pageInfo = new PageInfo<>(list);
    pageInfo.setTotal(total);
    pageInfo.setPageNum(page);
    pageInfo.setPageSize(rows);
    return GlobalResultGenerator.genSuccessResult(pageInfo);
}

@GetMapping("/category/{category}/count")
public GlobalResult<Integer> getProblemCountByCategory(
        @PathVariable String category) {
    int count = problemService.countProblems(null, category);
    return GlobalResultGenerator.genSuccessResult(count);
}

@GetMapping("/category/{category}/all")
public GlobalResult<List<ProblemDTO>> getAllProblemsByCategory(
        @PathVariable String category) {
    List<ProblemDTO> list = problemService.selectProblemsByCategory(category);
    return GlobalResultGenerator.genSuccessResult(list);
}

@GetMapping("/difficulty/{difficulty}")
public GlobalResult<PageInfo<ProblemDTO>> getProblemsByDifficulty(
        @PathVariable String difficulty,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "100") Integer rows) {
    // 使用优化后的分页查询方法
    List<ProblemDTO> list = problemService.selectProblemsByDifficultyWithPagination(difficulty, page, rows);
    // 获取总数
    int total = problemService.countProblems(difficulty, null);
    // 手动构建PageInfo对象
    PageInfo<ProblemDTO> pageInfo = new PageInfo<>(list);
    pageInfo.setTotal(total);
    pageInfo.setPageNum(page);
    pageInfo.setPageSize(rows);
    return GlobalResultGenerator.genSuccessResult(pageInfo);
}

@GetMapping("/difficulty/{difficulty}/count")
public GlobalResult<Integer> getProblemCountByDifficulty(
        @PathVariable String difficulty) {
    int count = problemService.countProblems(difficulty, null);
    return GlobalResultGenerator.genSuccessResult(count);
}

@GetMapping("/difficulty/{difficulty}/all")
public GlobalResult<List<ProblemDTO>> getAllProblemsByDifficulty(
        @PathVariable String difficulty) {
    List<ProblemDTO> list = problemService.selectProblemsByDifficulty(difficulty);
    return GlobalResultGenerator.genSuccessResult(list);
}

    @PostMapping("/post")
    @RequiresPermissions(value = "user")
    public GlobalResult<Long> postProblem(@RequestBody ProblemDTO problem) {
        return GlobalResultGenerator.genSuccessResult(problemService.postProblem(problem));
    }

    @PutMapping("/post")
    @AuthorshipInterceptor(moduleName = Module.PROBLEM)
    public GlobalResult<Long> updateProblem(@RequestBody ProblemDTO problem) {
        return GlobalResultGenerator.genSuccessResult(problemService.postProblem(problem));
    }

    @DeleteMapping("/delete/{id}")
    @AuthorshipInterceptor(moduleName = Module.PROBLEM)
    public GlobalResult<Integer> delete(@PathVariable Long id) {
        return GlobalResultGenerator.genSuccessResult(problemService.delete(id));
    }
    
    /**
     * 删除题目
     * 
     * @param id 题目ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @RequiresPermissions(value = "user")
    public GlobalResult<Void> deleteProblem(@PathVariable Long id) {
        try {
            // 参数验证
            if (id == null) {
                return GlobalResultGenerator.genErrorResult("题目ID不能为空");
            }
            
            // 调用服务层删除题目
            problemService.deleteProblem(id);
            return GlobalResultGenerator.genSuccessResult("删除成功");
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("删除题目失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加题目
     * 
     * @param problem 题目信息
     * @return 添加结果
     */
    @PostMapping
    @RequiresPermissions(value = "user")
    public GlobalResult<Long> addProblem(@RequestBody ProblemDTO problem) {
        try {
            // 参数验证
            if (problem.getTitle() == null || problem.getTitle().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("题目标题不能为空");
            }
            if (problem.getDifficulty() == null || problem.getDifficulty().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("题目难度不能为空");
            }
            if (problem.getDescription() == null || problem.getDescription().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("题目描述不能为空");
            }
            if (problem.getInputDescription() == null || problem.getInputDescription().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("输入描述不能为空");
            }
            if (problem.getOutputDescription() == null || problem.getOutputDescription().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("输出描述不能为空");
            }
            
            // 设置默认值
            if (problem.getSubmitCount() == null) {
                problem.setSubmitCount(0);
            }
            if (problem.getAcceptCount() == null) {
                problem.setAcceptCount(0);
            }
            if (problem.getAcceptanceRate() == null) {
                problem.setAcceptanceRate(0.0);
            }
            if (problem.getVisible() == null) {
                problem.setVisible(1);
            }
            
            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            problem.setCreatedTime(now);
            problem.setUpdatedTime(now);
            
            // 调用服务层添加题目
            Long problemId = problemService.postProblem(problem);
            return GlobalResultGenerator.genSuccessResult(problemId);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("添加题目失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新题目
     * 
     * @param id 题目ID
     * @param problem 题目信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    @RequiresPermissions(value = "user")
    public GlobalResult<Void> updateProblem(@PathVariable Long id, @RequestBody ProblemDTO problem) {
        try {
            // 参数验证
            if (id == null) {
                return GlobalResultGenerator.genErrorResult("题目ID不能为空");
            }
            if (problem.getTitle() == null || problem.getTitle().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("题目标题不能为空");
            }
            if (problem.getDifficulty() == null || problem.getDifficulty().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("题目难度不能为空");
            }
            if (problem.getDescription() == null || problem.getDescription().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("题目描述不能为空");
            }
            if (problem.getInputDescription() == null || problem.getInputDescription().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("输入描述不能为空");
            }
            if (problem.getOutputDescription() == null || problem.getOutputDescription().isEmpty()) {
                return GlobalResultGenerator.genErrorResult("输出描述不能为空");
            }
            
            // 设置ID和更新时间
            problem.setId(id);
            problem.setUpdatedTime(LocalDateTime.now());
            
            // 调用服务层更新题目
            problemService.updateProblem(problem);
            return GlobalResultGenerator.genSuccessResult("更新成功");
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("更新题目失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存题目评测结果和AI评价结果到Redis
     *
     * @param requestBody 包含chatId、branchId和data的请求体
     * @return 操作结果
     */
    @PostMapping("/save-results")
    public GlobalResult<Void> saveResults(@RequestBody Map<String, Object> requestBody) {
        try {
            // 从请求体中获取参数
            String chatId = (String) requestBody.get("chatId");
            String branchId = (String) requestBody.get("branchId");
            
            // 使用FastJSON格式化输出data对象
            Object dataObj = requestBody.get("data");
            System.out.println("Data对象类型: " + (dataObj != null ? dataObj.getClass().getName() : "null"));
            System.out.println("Data内容: " + com.alibaba.fastjson.JSON.toJSONString(dataObj, true));
            
            Map<String, Object> data = (Map<String, Object>) dataObj;
            
            // 参数验证
            if (chatId == null || branchId == null || data == null) {
                return GlobalResultGenerator.genErrorResult("参数不完整，需要提供chatId、branchId和data");
            }
            
            // 输出data中的键值对
            System.out.println("Data中的键值对:");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
            
            // 构建Redis键名
            String redisKey = "problem_results:" + chatId + ":" + branchId;
            
            // 将数据保存到Redis，有效期设置为60分钟
            redisService.set(redisKey, data, 60 * 60);
            
            return GlobalResultGenerator.genSuccessResult("保存成功");
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("保存结果失败: " + e.getMessage());
        }
    }
    
    /**
     * 从Redis获取题目评测结果和AI评价结果
     *
     * @param chatId 聊天ID
     * @param branchId 分支ID
     * @return 评测结果和AI评价结果
     */
    @GetMapping("/get-results")
    public GlobalResult<Map<String, Object>> getResults(
            @RequestParam("chatId") String chatId,
            @RequestParam("branchId") String branchId) {
        try {
            // 参数验证
            if (chatId == null || chatId.isEmpty() || branchId == null || branchId.isEmpty()) {
                return GlobalResultGenerator.genErrorResult("参数不完整，需要提供chatId和branchId");
            }
            
            // 构建Redis键名
            String redisKey = "problem_results:" + chatId + ":" + branchId;
            
            // 从Redis获取数据
            // 从Redis获取数据并使用FastJSON反序列化为Map
            String jsonStr = (String) redisService.get(redisKey);
            Map<String, Object> data = com.alibaba.fastjson.JSON.parseObject(jsonStr);
            
            if (data == null) {
                return GlobalResultGenerator.genErrorResult("未找到相关结果数据");
            }
            
            return GlobalResultGenerator.genSuccessResult(data);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("获取结果失败: " + e.getMessage());
        }
    }
}
package com.sdumagicode.backend.controller.oj;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sdumagicode.backend.core.exception.BusinessException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.core.service.security.annotation.AuthorshipInterceptor;
import com.sdumagicode.backend.dto.ProblemDTO;
import com.sdumagicode.backend.enumerate.Module;
import com.sdumagicode.backend.service.ProblemService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {
    @Autowired
    private ProblemService problemService;

    @GetMapping
    public GlobalResult<PageInfo<ProblemDTO>> getProblemList(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer rows) {
        PageHelper.startPage(page, rows);
        List<ProblemDTO> list = problemService.selectProblems(difficulty, category);
        PageInfo<ProblemDTO> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
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
            @RequestParam(defaultValue = "10") Integer rows) {
        PageHelper.startPage(page, rows);
        List<ProblemDTO> list = problemService.selectProblemsByCategory(category);
        PageInfo<ProblemDTO> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
    }

    @GetMapping("/difficulty/{difficulty}")
    public GlobalResult<PageInfo<ProblemDTO>> getProblemsByDifficulty(
            @PathVariable String difficulty,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer rows) {
        PageHelper.startPage(page, rows);
        List<ProblemDTO> list = problemService.selectProblemsByDifficulty(difficulty);
        PageInfo<ProblemDTO> pageInfo = new PageInfo<>(list);
        return GlobalResultGenerator.genSuccessResult(pageInfo);
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
}
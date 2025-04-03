package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.dto.ProblemDTO;
import com.sdumagicode.backend.entity.Problem;
import com.sdumagicode.backend.mapper.ProblemMapper;
import com.sdumagicode.backend.service.ProblemService;
import com.sdumagicode.backend.util.BeanCopierUtil;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Condition;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * 题目服务实现类
 * 继承AbstractService基类并实现ProblemService接口
 * 
 * @author ronger
 * @email ronger-x@outlook.com
 */
@Service
public class ProblemServiceImpl extends AbstractService<Problem> implements ProblemService {
    @Resource
    private ProblemMapper problemMapper;

    @Override
    public void save(Problem model) {
        super.save(model);
    }

    @Override
    public void save(List<Problem> models) {
        super.save(models);
    }

    @Override
    public void deleteById(String id) {
        super.deleteById(id);
    }

    @Override
    public void deleteByIds(String ids) {
        super.deleteByIds(ids);
    }

    @Override
    public void update(Problem model) {
        super.update(model);
    }

    @Override
    public Problem findById(String id) {
        return super.findById(id);
    }

    @Override
    public Problem findBy(String fieldName, Object value) throws TooManyResultsException {
        return super.findBy(fieldName, value);
    }

    @Override
    public List<Problem> findByIds(String ids) {
        return super.findByIds(ids);
    }

    @Override
    public List<Problem> findByCondition(Condition condition) {
        return super.findByCondition(condition);
    }

    @Override
    public List<Problem> findAll() {
        return super.findAll();
    }

    /**
     * 根据ID查询题目详情
     * @param id 题目ID
     * @return 题目DTO
     */
    @Override
    public ProblemDTO getProblemById(Long id) {
        Problem problem = problemMapper.selectByPrimaryKey(id);
        ProblemDTO problemDTO = new ProblemDTO();
        BeanCopierUtil.convert(problem, problemDTO);
        return problemDTO;
    }

    @Override
    public List<ProblemDTO> selectProblems(String difficulty, String category) {
        Condition condition = new Condition(Problem.class);
        if (difficulty != null) {
            condition.createCriteria().andEqualTo("difficulty", difficulty);
        }
        if (category != null) {
            condition.createCriteria().andEqualTo("category", category);
        }
        List<Problem> problems = this.findByCondition(condition);
        return problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProblemDTO> selectProblemsByCategory(String category) {
        Condition condition = new Condition(Problem.class);
        condition.createCriteria().andEqualTo("category", category);
        List<Problem> problems = this.findByCondition(condition);
        return problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProblemDTO> selectProblemsByDifficulty(String difficulty) {
        Condition condition = new Condition(Problem.class);
        condition.createCriteria().andEqualTo("difficulty", difficulty);
        List<Problem> problems = this.findByCondition(condition);
        return problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Long postProblem(ProblemDTO problemDTO) {
        Problem problem = new Problem();
        BeanCopierUtil.convert(problemDTO, problem);
        this.save(problem);
        return problem.getId();
    }

    @Override
    public Integer delete(Long id) {
        this.deleteById(String.valueOf(id));
        return 1;
    }

    /**
     * 新增题目
     * @param problemDTO 题目信息DTO
     */
    @Override
    public void addProblem(ProblemDTO problemDTO) {
        Problem problem = new Problem();
        BeanCopierUtil.convert(problemDTO, problem);
        this.save(problem);
    }

    /**
     * 更新题目
     * @param problemDTO 题目信息DTO
     */
    @Override
    public void updateProblem(ProblemDTO problemDTO) {
        Problem problem = new Problem();
        BeanCopierUtil.convert(problemDTO, problem);
        this.update(problem);
    }

    /**
     * 删除题目
     * @param id 题目ID
     */
    @Override
    public void deleteProblem(Long id) {
        this.deleteById(String.valueOf(id));
    }

    /**
     * 查询题目列表
     * @return 题目DTO列表
     */
    public List<ProblemDTO> findProblems() {
        List<Problem> problems = this.findAll();
        return problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public ProblemDTO selectProblemById(Long id) {
        Problem problem = problemMapper.selectByPrimaryKey(id);
        ProblemDTO problemDTO = new ProblemDTO();
        BeanCopierUtil.convert(problem, problemDTO);
        return problemDTO;
    }

    @Override
    public List<String> getAllTags() {
        return problemMapper.selectAllTags();
    }

    @Override
    public Map<String, Object> getStatistics() {
        List<Map<String, Object>> stats = problemMapper.getAcceptanceStatistics();
        Map<String, Object> result = new HashMap<>();
        
        int totalProblems = 0;
        int totalSolved = 0;
        int totalAttempted = 0;
        double avgAcceptanceRate = 0.0;
        
        // 计算总数和已解决数
        for (Map<String, Object> stat : stats) {
            int total = ((Number) stat.get("total")).intValue();
            int solved = ((Number) stat.get("solved")).intValue();
            double avgRate = ((Number) stat.get("avg_rate")).doubleValue();
            
            totalProblems += total;
            totalSolved += solved;
            // 累加每个难度的平均通过率，后面再取平均
            avgAcceptanceRate += avgRate * total; // 根据题目数量加权
        }
        
        // 计算总体平均通过率
        avgAcceptanceRate = totalProblems > 0 ? avgAcceptanceRate / totalProblems : 0;
        
        // 构造前端需要的数据结构
        Map<String, Object> totalStat = new HashMap<>();
        totalStat.put("title", "总题数");
        totalStat.put("value", totalProblems);
        totalStat.put("rate", Math.round(avgAcceptanceRate)); // 使用加权平均通过率
        
        Map<String, Object> solvedStat = new HashMap<>();
        solvedStat.put("title", "已解决");
        solvedStat.put("value", totalSolved);
        solvedStat.put("rate", totalProblems > 0 ? Math.round((double) totalSolved / totalProblems * 100) : 0);
        
        Map<String, Object> attemptedStat = new HashMap<>();
        attemptedStat.put("title", "尝试过");
        attemptedStat.put("value", totalSolved); // 使用已解决数作为尝试过的数量
        attemptedStat.put("rate", totalProblems > 0 ? Math.round((double) totalSolved / totalProblems * 100) : 0);
        
        result.put("total", totalStat);
        result.put("solved", solvedStat);
        result.put("attempted", attemptedStat);
        
        return result;
    }
}
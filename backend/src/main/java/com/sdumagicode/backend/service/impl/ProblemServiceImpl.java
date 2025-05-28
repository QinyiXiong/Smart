package com.sdumagicode.backend.service.impl;

import com.github.pagehelper.PageHelper;
import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.dto.ProblemDTO;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.entity.Problem;
import com.sdumagicode.backend.mapper.CodeSubmissionMapper;
import com.sdumagicode.backend.mapper.ProblemMapper;
import com.sdumagicode.backend.service.ProblemService;
import com.sdumagicode.backend.util.BeanCopierUtil;
import com.sdumagicode.backend.util.UserUtils;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Condition;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题目服务实现类
 * 继承AbstractService基类并实现ProblemService接口
 * 
 * @author ronger
 * @email ronger-x@outlook.com
 */
@Service
public class ProblemServiceImpl extends AbstractService<Problem> implements ProblemService {
    private static final Logger logger = LoggerFactory.getLogger(ProblemServiceImpl.class);
    
    // 缓存相关常量
    private static final String CACHE_PROBLEM_PREFIX = "problem:";
    private static final String CACHE_PROBLEMS_ALL = "problems:all";
    private static final String CACHE_PROBLEMS_DIFFICULTY = "problems:difficulty:";
    private static final String CACHE_PROBLEMS_CATEGORY = "problems:category:";
    private static final String CACHE_PROBLEMS_TAGS = "problems:tags";
    private static final String CACHE_PROBLEMS_STATISTICS = "problems:statistics";
    private static final long CACHE_EXPIRE_TIME = 30; // 缓存过期时间（分钟）
    
    @Resource
    private ProblemMapper problemMapper;
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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
    public List<ProblemDTO> selectProblemsByCategory(String category) {
        Condition condition = new Condition(Problem.class);
        // 创建一个Criteria对象，然后添加条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("category", category);
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
        // 创建一个Criteria对象，然后添加条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("difficulty", difficulty);
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
    public List<ProblemDTO> selectProblemsWithPagination(String difficulty, String category, int page, int size) {
        String cacheKey = CACHE_PROBLEMS_ALL;
        if (difficulty != null) {
            cacheKey += ":difficulty:" + difficulty;
        }
        if (category != null) {
            cacheKey += ":category:" + category;
        }
        cacheKey += ":page:" + page + ":size:" + size;
        
        // 尝试从缓存获取
        List<ProblemDTO> cachedProblems = (List<ProblemDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProblems != null) {
            logger.info("从缓存获取题目列表，key={}", cacheKey);
            return cachedProblems;
        }
        
        // 缓存未命中，从数据库查询
        Condition condition = new Condition(Problem.class);
        // 创建一个Criteria对象，然后添加所有条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        if (difficulty != null) {
            criteria.andEqualTo("difficulty", difficulty);
        }
        if (category != null) {
            criteria.andEqualTo("category", category);
        }
        
        // 设置排序
        condition.setOrderByClause("id ASC");
        
        // 使用PageHelper进行物理分页，直接从数据库获取分页结果
        PageHelper.startPage(page, size);
        List<Problem> problems = this.findByCondition(condition);
        
        List<ProblemDTO> result = problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // 将结果存入缓存，设置较短的过期时间以保证数据及时更新
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        
        return result;
    }
    
    @Override
    public List<ProblemDTO> selectProblems(String difficulty, String category) {
        String cacheKey = CACHE_PROBLEMS_ALL;
        if (difficulty != null) {
            cacheKey += ":difficulty:" + difficulty;
        }
        if (category != null) {
            cacheKey += ":category:" + category;
        }
        
        // 尝试从缓存获取
        List<ProblemDTO> cachedProblems = (List<ProblemDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProblems != null) {
            logger.info("从缓存获取所有题目，key={}", cacheKey);
            return cachedProblems;
        }
        
        // 缓存未命中，从数据库查询
        Condition condition = new Condition(Problem.class);
        // 创建一个Criteria对象，然后添加所有条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        if (difficulty != null) {
            criteria.andEqualTo("difficulty", difficulty);
        }
        if (category != null) {
            criteria.andEqualTo("category", category);
        }
        List<Problem> problems = this.findByCondition(condition);
        
        List<ProblemDTO> result = problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // 将结果存入缓存
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return result;
    }
    
    @Override
    public List<ProblemDTO> selectProblemsByCategoryWithPagination(String category, int page, int size) {
        String cacheKey = CACHE_PROBLEMS_CATEGORY + category + ":page:" + page + ":size:" + size;
        
        // 尝试从缓存获取
        List<ProblemDTO> cachedProblems = (List<ProblemDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProblems != null) {
            logger.info("从缓存获取分类题目列表，key={}", cacheKey);
            return cachedProblems;
        }
        
        // 缓存未命中，从数据库查询
        Condition condition = new Condition(Problem.class);
        // 创建一个Criteria对象，然后添加条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("category", category);
        
        // 设置排序
        condition.setOrderByClause("id ASC");
        
        // 使用PageHelper进行物理分页，直接从数据库获取分页结果
        PageHelper.startPage(page, size);
        List<Problem> problems = this.findByCondition(condition);
        
        List<ProblemDTO> result = problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // 将结果存入缓存，设置较短的过期时间
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        
        return result;
    }
    
    @Override
    public List<ProblemDTO> selectProblemsByDifficultyWithPagination(String difficulty, int page, int size) {
        String cacheKey = CACHE_PROBLEMS_DIFFICULTY + difficulty + ":page:" + page + ":size:" + size;
        
        // 尝试从缓存获取
        List<ProblemDTO> cachedProblems = (List<ProblemDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProblems != null) {
            logger.info("从缓存获取难度题目列表，key={}", cacheKey);
            return cachedProblems;
        }
        
        // 缓存未命中，从数据库查询
        Condition condition = new Condition(Problem.class);
        // 创建一个Criteria对象，然后添加条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        criteria.andEqualTo("difficulty", difficulty);
        
        // 设置排序
        condition.setOrderByClause("id ASC");
        
        // 使用PageHelper进行物理分页，直接从数据库获取分页结果
        PageHelper.startPage(page, size);
        List<Problem> problems = this.findByCondition(condition);
        
        List<ProblemDTO> result = problems.stream()
                .map(problem -> {
                    ProblemDTO dto = new ProblemDTO();
                    BeanCopierUtil.convert(problem, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // 将结果存入缓存，设置较短的过期时间
        redisTemplate.opsForValue().set(cacheKey, result, 5, TimeUnit.MINUTES);
        
        return result;
    }
    
    @Override
    public int countProblems(String difficulty, String category) {
        String cacheKey = "count:problems";
        if (difficulty != null) {
            cacheKey += ":difficulty:" + difficulty;
        }
        if (category != null) {
            cacheKey += ":category:" + category;
        }
        
        // 尝试从缓存获取
        Integer cachedCount = (Integer) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            return cachedCount;
        }
        
        // 缓存未命中，从数据库查询
        Condition condition = new Condition(Problem.class);
        // 创建一个Criteria对象，然后添加所有条件
        tk.mybatis.mapper.entity.Example.Criteria criteria = condition.createCriteria();
        // 只查询可见的题目
        criteria.andEqualTo("visible", 1);
        if (difficulty != null) {
            criteria.andEqualTo("difficulty", difficulty);
        }
        if (category != null) {
            criteria.andEqualTo("category", category);
        }
        
        int count = problemMapper.selectCountByCondition(condition);
        logger.info("从数据库查询题目总数: {}", count);
        
        // 将结果存入缓存
        redisTemplate.opsForValue().set(cacheKey, count, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return count;
    }
    
    @Override
    public ProblemDTO selectProblemById(Long id) {
        String cacheKey = CACHE_PROBLEM_PREFIX + id;
        
        // 尝试从缓存获取
        ProblemDTO cachedProblem = (ProblemDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProblem != null) {
            logger.info("从缓存获取题目详情，id={}", id);
            return cachedProblem;
        }
        
        // 缓存未命中，从数据库查询
        Problem problem = problemMapper.selectByPrimaryKey(id);
        ProblemDTO problemDTO = new ProblemDTO();
        BeanCopierUtil.convert(problem, problemDTO);
        
        // 将结果存入缓存
        redisTemplate.opsForValue().set(cacheKey, problemDTO, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return problemDTO;
    }

    @Override
    public List<String> getAllTags() {
        // 尝试从缓存获取
        List<String> cachedTags = (List<String>) redisTemplate.opsForValue().get(CACHE_PROBLEMS_TAGS);
        if (cachedTags != null) {
            logger.info("从缓存获取所有标签");
            return cachedTags;
        }
        
        // 缓存未命中，从数据库查询
        List<String> tags = problemMapper.selectAllTags();
        
        // 将结果存入缓存
        redisTemplate.opsForValue().set(CACHE_PROBLEMS_TAGS, tags, CACHE_EXPIRE_TIME, TimeUnit.MINUTES);
        
        return tags;
    }

    @Resource
    private CodeSubmissionMapper codeSubmissionMapper;
    
    @Override
    public Map<String, Object> getStatistics() {
        // 直接从数据库查询，不使用缓存
        logger.info("从数据库查询统计信息");
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取当前登录用户ID
        Long userId = null;
        try {
            userId = UserUtils.getCurrentUserByToken().getIdUser();
            logger.info("当前用户ID: {}", userId);
        } catch (Exception e) {
            logger.warn("获取当前用户ID失败，将返回全局统计数据", e);
        }
        
        // 直接查询可见题目总数
        Condition condition = new Condition(Problem.class);
        condition.createCriteria().andEqualTo("visible", 1);
        int totalProblems = problemMapper.selectCountByCondition(condition);
        logger.info("可见题目总数: {}", totalProblems);
        
        // 获取用户提交记录
        int acceptedProblems = 0;
        int totalAttempted = 0;
        
        if (userId != null) {
            // 查询用户提交记录
            // 使用Example查询用户的所有提交记录
            Example example = new Example(CodeSubmission.class);
            example.selectProperties("problemId", "status");
            example.setDistinct(true);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("userId", userId);
            
            List<CodeSubmission> submissions = codeSubmissionMapper.selectByExample(example);
            logger.info("用户提交记录数: {}", submissions.size());
            
            // 统计已尝试和已通过的题目数量
            Set<Long> attemptedProblemIds = new HashSet<>();
            Set<Long> acceptedProblemIds = new HashSet<>();
            
            for (CodeSubmission submission : submissions) {
                attemptedProblemIds.add(submission.getProblemId());
                if ("accepted".equals(submission.getStatus())) {
                    acceptedProblemIds.add(submission.getProblemId());
                }
            }
            
            acceptedProblems = acceptedProblemIds.size();
            totalAttempted = attemptedProblemIds.size();
            logger.info("用户已解决题目数: {}, 尝试过的题目数: {}", acceptedProblems, totalAttempted);
        }
        
        // 构造前端需要的数据结构
        Map<String, Object> totalStat = new HashMap<>();
        totalStat.put("title", "总题数");
        totalStat.put("value", totalProblems);
        totalStat.put("rate", 100); // 总题数的比率为100%
        
        Map<String, Object> solvedStat = new HashMap<>();
        solvedStat.put("title", "已解决");
        solvedStat.put("value", acceptedProblems);
        solvedStat.put("rate", totalProblems > 0 ? Math.round((double) acceptedProblems / totalProblems * 100) : 0);
        
        Map<String, Object> attemptedStat = new HashMap<>();
        attemptedStat.put("title", "尝试过");
        attemptedStat.put("value", totalAttempted);
        attemptedStat.put("rate", totalProblems > 0 ? Math.round((double) totalAttempted / totalProblems * 100) : 0);
        
        result.put("total", totalStat);
        result.put("solved", solvedStat);
        result.put("attempted", attemptedStat);
        
        // 不再将结果存入缓存，每次都从数据库获取最新数据
        
        return result;
    }
    
    @Override
    public void refreshCache() {
        logger.info("刷新题目相关缓存");
        // 删除所有题目相关缓存
        redisTemplate.delete(redisTemplate.keys(CACHE_PROBLEM_PREFIX + "*"));
        redisTemplate.delete(redisTemplate.keys(CACHE_PROBLEMS_ALL + "*"));
        redisTemplate.delete(redisTemplate.keys(CACHE_PROBLEMS_DIFFICULTY + "*"));
        redisTemplate.delete(redisTemplate.keys(CACHE_PROBLEMS_CATEGORY + "*"));
        redisTemplate.delete(CACHE_PROBLEMS_TAGS);
        redisTemplate.delete(CACHE_PROBLEMS_STATISTICS);
    }
}
package com.sdumagicode.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdumagicode.backend.dto.CodeRunDTO;
import com.sdumagicode.backend.dto.CodeSubmitDTO;
import com.sdumagicode.backend.dto.JudgeResultDTO;
import com.sdumagicode.backend.dto.TestCaseDTO;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.entity.Problem;
import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.mapper.CodeSubmissionMapper;
import com.sdumagicode.backend.mapper.ProblemMapper;
import com.sdumagicode.backend.service.JudgeService;
import com.sdumagicode.backend.util.UserUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 代码评测服务实现类
 */
@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 代码文件扩展名映射
    private static final Map<String, String> LANGUAGE_EXTENSIONS = new HashMap<>();
    static {
        LANGUAGE_EXTENSIONS.put("java", ".java");
        LANGUAGE_EXTENSIONS.put("cpp", ".cpp");
        LANGUAGE_EXTENSIONS.put("python", ".py");
    }

    // 编译命令映射
    private static final Map<String, String[]> COMPILE_COMMANDS = new HashMap<>();
    static {
        COMPILE_COMMANDS.put("java", new String[]{"javac", "{filename}"});
        COMPILE_COMMANDS.put("cpp", new String[]{"g++", "-o", "{output}", "{filename}", "-std=c++11"});
    }

    // 运行命令映射
    private static final Map<String, String[]> RUN_COMMANDS = new HashMap<>();
    static {
        RUN_COMMANDS.put("java", new String[]{"java", "-cp", "{dir}", "Main"});
        RUN_COMMANDS.put("cpp", new String[]{"{output}"});
        RUN_COMMANDS.put("python", new String[]{"python", "{filename}"});
    }

    @Override
    public JudgeResultDTO runCode(CodeRunDTO codeRunDTO) {
        try {
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("judge_");
            String language = codeRunDTO.getLanguage();
            String code = codeRunDTO.getCode();
            String input = codeRunDTO.getInput();

            // 保存代码到文件
            String filename = getCodeFilename(language);
            Path codePath = tempDir.resolve(filename);
            Files.write(codePath, code.getBytes(StandardCharsets.UTF_8));

            // 保存输入到文件
            Path inputPath = tempDir.resolve("input.txt");
            Files.write(inputPath, input.getBytes(StandardCharsets.UTF_8));

            // 编译代码（如果需要）
            if (COMPILE_COMMANDS.containsKey(language)) {
                JudgeResultDTO compileResult = compileCode(language, codePath, tempDir);
                if (!"ACCEPTED".equals(compileResult.getStatus())) {
                    return compileResult;
                }
            }

            // 运行代码
            return executeCode(language, codePath, inputPath, tempDir, null);

        } catch (Exception e) {
            log.error("运行代码失败", e);
            return JudgeResultDTO.builder()
                    .status("SYSTEM_ERROR")
                    .errorMessage("系统错误: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public JudgeResultDTO submitCode(CodeSubmitDTO codeSubmitDTO) {
        try {
            // 获取当前用户
            User currentUser = UserUtils.getCurrentUserByToken();
            if (currentUser == null) {
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("用户未登录")
                        .build();
            }

            // 获取题目信息
            Problem problem = problemMapper.selectByPrimaryKey(codeSubmitDTO.getProblemId());
            if (problem == null) {
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("题目不存在")
                        .build();
            }

            // 解析测试用例
            List<TestCaseDTO> testCases = parseTestCases(problem.getTestCases());
            if (testCases.isEmpty()) {
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("题目没有测试用例")
                        .build();
            }

            // 创建提交记录
            CodeSubmission submission = new CodeSubmission();
            submission.setProblemId(codeSubmitDTO.getProblemId());
            submission.setUserId(currentUser.getIdUser());
            submission.setCode(codeSubmitDTO.getCode());
            submission.setLanguage(codeSubmitDTO.getLanguage());
            submission.setStatus("JUDGING");
            submission.setTotalTestCases(testCases.size());
            codeSubmissionMapper.insert(submission);

            // 异步执行评测
            executeJudgeAsync(submission, testCases, problem);

            // 返回初始结果，包含submissionId
            return JudgeResultDTO.builder()
                    .status("JUDGING")
                    .submissionId(submission.getId())
                    .totalTestCases(testCases.size())
                    .build();

        } catch (Exception e) {
            log.error("提交代码失败", e);
            return JudgeResultDTO.builder()
                    .status("SYSTEM_ERROR")
                    .errorMessage("系统错误: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public List<CodeSubmission> getSubmissionHistory(Long problemId, Long userId) {
        // 查询提交历史
        CodeSubmission example = new CodeSubmission();
        example.setProblemId(problemId);
        if (userId != null) {
            example.setUserId(userId);
        }
        return codeSubmissionMapper.select(example);
    }

    /**
     * 异步执行评测
     */
    @Async
    protected void executeJudgeAsync(CodeSubmission submission, List<TestCaseDTO> testCases, Problem problem) {
        try {
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("judge_");
            String language = submission.getLanguage();
            String code = submission.getCode();

            // 保存代码到文件
            String filename = getCodeFilename(language);
            Path codePath = tempDir.resolve(filename);
            Files.write(codePath, code.getBytes(StandardCharsets.UTF_8));

            // 编译代码（如果需要）
            if (COMPILE_COMMANDS.containsKey(language)) {
                JudgeResultDTO compileResult = compileCode(language, codePath, tempDir);
                if (!"ACCEPTED".equals(compileResult.getStatus())) {
                    updateSubmissionResult(submission, compileResult);
                    return;
                }
            }

            // 执行所有测试用例
            int passedCount = 0;
            int totalTime = 0;
            int maxMemory = 0;
            StringBuilder errorOutput = new StringBuilder();

            for (int i = 0; i < testCases.size(); i++) {
                TestCaseDTO testCase = testCases.get(i);
                
                // 保存测试输入到文件
                Path inputPath = tempDir.resolve("input.txt");
                Files.write(inputPath, testCase.getInput().getBytes(StandardCharsets.UTF_8));

                // 运行代码
                JudgeResultDTO result = executeCode(language, codePath, inputPath, tempDir, problem);
                
                // 更新统计信息
                totalTime += result.getTime() != null ? result.getTime() : 0;
                maxMemory = Math.max(maxMemory, result.getMemory() != null ? result.getMemory() : 0);
                
                // 检查结果
                if ("ACCEPTED".equals(result.getStatus())) {
                    // 比较输出
                    if (compareOutput(result.getOutput(), testCase.getOutput())) {
                        passedCount++;
                    } else {
                        errorOutput.append("测试点 ").append(i + 1).append(" 输出不匹配\n");
                        errorOutput.append("预期输出: \n").append(testCase.getOutput()).append("\n");
                        errorOutput.append("实际输出: \n").append(result.getOutput()).append("\n\n");
                    }
                } else {
                    errorOutput.append("测试点 ").append(i + 1).append(" 执行失败: ").append(result.getStatus()).append("\n");
                    errorOutput.append(result.getErrorMessage()).append("\n\n");
                }
            }

            // 构建最终结果
            JudgeResultDTO finalResult = JudgeResultDTO.builder()
                    .status(passedCount == testCases.size() ? "ACCEPTED" : "WRONG_ANSWER")
                    .time(totalTime / testCases.size()) // 平均时间
                    .memory(maxMemory)
                    .passedTestCases(passedCount)
                    .totalTestCases(testCases.size())
                    .errorMessage(errorOutput.length() > 0 ? errorOutput.toString() : null)
                    .build();

            // 更新提交记录
            updateSubmissionResult(submission, finalResult);

            // 更新题目统计信息
            updateProblemStatistics(problem, "ACCEPTED".equals(finalResult.getStatus()));

        } catch (Exception e) {
            log.error("评测执行失败", e);
            JudgeResultDTO errorResult = JudgeResultDTO.builder()
                    .status("SYSTEM_ERROR")
                    .errorMessage("系统错误: " + e.getMessage())
                    .build();
            updateSubmissionResult(submission, errorResult);
        }
    }

    /**
     * 编译代码
     */
    private JudgeResultDTO compileCode(String language, Path codePath, Path tempDir) {
        try {
            String[] commandTemplate = COMPILE_COMMANDS.get(language);
            if (commandTemplate == null) {
                return JudgeResultDTO.builder().status("ACCEPTED").build();
            }

            String[] command = new String[commandTemplate.length];
            for (int i = 0; i < commandTemplate.length; i++) {
                command[i] = commandTemplate[i]
                        .replace("{filename}", codePath.toString())
                        .replace("{output}", tempDir.resolve("program").toString())
                        .replace("{dir}", tempDir.toString());
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return JudgeResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .errorMessage("编译超时")
                        .build();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                return JudgeResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .errorMessage(output.toString())
                        .build();
            }

            return JudgeResultDTO.builder().status("ACCEPTED").build();

        } catch (Exception e) {
            log.error("编译代码失败", e);
            return JudgeResultDTO.builder()
                    .status("COMPILATION_ERROR")
                    .errorMessage("编译错误: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 执行代码
     */
    private JudgeResultDTO executeCode(String language, Path codePath, Path inputPath, Path tempDir, Problem problem) {
        try {
            String[] commandTemplate = RUN_COMMANDS.get(language);
            if (commandTemplate == null) {
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("不支持的语言: " + language)
                        .build();
            }

            String[] command = new String[commandTemplate.length];
            for (int i = 0; i < commandTemplate.length; i++) {
                command[i] = commandTemplate[i]
                        .replace("{filename}", codePath.toString())
                        .replace("{output}", tempDir.resolve("program").toString())
                        .replace("{dir}", tempDir.toString());
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(tempDir.toFile());
            pb.redirectInput(inputPath.toFile());
            pb.redirectErrorStream(true);

            // 设置时间限制
            int timeLimit = problem != null ? problem.getTimeLimit() : 5000; // 默认5秒

            // 开始计时
            long startTime = System.currentTimeMillis();
            Process process = pb.start();
            
            // 等待进程完成或超时
            boolean completed = process.waitFor(timeLimit, TimeUnit.MILLISECONDS);
            long endTime = System.currentTimeMillis();
            int executionTime = (int) (endTime - startTime);

            if (!completed) {
                process.destroyForcibly();
                return JudgeResultDTO.builder()
                        .status("TIME_LIMIT_EXCEEDED")
                        .time(executionTime)
                        .errorMessage("执行超时")
                        .build();
            }

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return JudgeResultDTO.builder()
                        .status("RUNTIME_ERROR")
                        .time(executionTime)
                        .errorMessage("运行时错误: " + output.toString())
                        .build();
            }

            // 估算内存使用（实际应该使用更精确的方法）
            int memoryUsage = 50; // 默认值，实际应该监控进程内存

            return JudgeResultDTO.builder()
                    .status("ACCEPTED")
                    .time(executionTime)
                    .memory(memoryUsage)
                    .output(output.toString())
                    .build();

        } catch (Exception e) {
            log.error("执行代码失败", e);
            return JudgeResultDTO.builder()
                    .status("SYSTEM_ERROR")
                    .errorMessage("执行错误: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 比较输出结果
     */
    private boolean compareOutput(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        
        // 规范化输出（去除末尾空白字符和空行）
        String normalizedActual = normalizeOutput(actual);
        String normalizedExpected = normalizeOutput(expected);
        
        return normalizedActual.equals(normalizedExpected);
    }
    
    /**
     * 规范化输出字符串
     */
    private String normalizeOutput(String output) {
        if (output == null) {
            return "";
        }
        
        // 按行分割
        String[] lines = output.split("\\r?\\n");
        
        // 处理每行（去除末尾空白字符）
        List<String> normalizedLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                normalizedLines.add(trimmed);
            }
        }
        
        // 重新组合
        return String.join("\n", normalizedLines);
    }

    /**
     * 获取代码文件名
     */
    private String getCodeFilename(String language) {
        switch (language) {
            case "java":
                return "Main.java";
            case "cpp":
                return "main.cpp";
            case "python":
                return "main.py";
            default:
                return "code" + LANGUAGE_EXTENSIONS.getOrDefault(language, ".txt");
        }
    }

    /**
     * 解析测试用例
     */
    private List<TestCaseDTO> parseTestCases(String testCasesJson) {
        if (testCasesJson == null || testCasesJson.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(testCasesJson, new TypeReference<List<TestCaseDTO>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析测试用例失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 更新提交记录结果
     */
    private void updateSubmissionResult(CodeSubmission submission, JudgeResultDTO result) {
        submission.setStatus(result.getStatus());
        submission.setExecutionTime(result.getTime());
        submission.setMemoryUsage(result.getMemory());
        submission.setErrorMessage(result.getErrorMessage());
        submission.setPassedTestCases(result.getPassedTestCases());
        codeSubmissionMapper.updateByPrimaryKey(submission);
    }

    /**
     * 更新题目统计信息
     */
    private void updateProblemStatistics(Problem problem, boolean accepted) {
        problem.setSubmitCount(problem.getSubmitCount() + 1);
        if (accepted) {
            problem.setAcceptCount(problem.getAcceptCount() + 1);
        }
        problem.setAcceptanceRate((double) problem.getAcceptCount() / problem.getSubmitCount());
        problemMapper.updateByPrimaryKey(problem);
    }
}
package com.sdumagicode.backend.service.impl;

import com.fasterxml.jackson.core.JsonParser;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 代码评测服务实现类
 */
@Service
@Slf4j
public class JudgeServiceImpl implements JudgeService {

    @Autowired
    private com.sdumagicode.backend.service.DockerService dockerService;
    
    /**
     * 初始化方法，在服务启动时输出Docker评测信息
     */
    @javax.annotation.PostConstruct
    public void init() {
        log.info("===== 代码评测服务初始化 =====");
        log.info("使用Docker容器进行代码评测");
        log.info("支持的语言: Java, C++, Python");
        log.info("请确保Docker服务已正确安装并启动");
        log.info("================================");
    }

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private CodeSubmissionMapper codeSubmissionMapper;
    
    @Autowired
    private com.sdumagicode.backend.service.ChatService chatService;

    private final ObjectMapper objectMapper;

    public JudgeServiceImpl() {
        // 初始化ObjectMapper并配置以处理特殊字符
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        this.objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
    }

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
    
    // 本地编译器路径配置和运行命令已移除，仅使用Docker进行代码评测
    // 支持的语言: Java, C++, Python

    @Override
    public JudgeResultDTO runCode(CodeRunDTO codeRunDTO) {
        try {
            // 检查Docker是否可用
            if (!dockerService.isDockerAvailable()) {
                log.error("Docker不可用，无法运行代码");
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("系统错误: Docker服务不可用，请确保Docker已正确安装并启动")
                        .build();
            }
            
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

            // 创建Docker容器
            String containerId = dockerService.createContainer(language, tempDir);
            if (containerId == null) {
                log.error("创建Docker容器失败");
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("系统错误: 创建Docker容器失败")
                        .build();
            }
            
            log.info("使用Docker容器运行代码: {}", containerId);
            
            try {
                // 编译代码（如果需要）
                if (COMPILE_COMMANDS.containsKey(language)) {
                    JudgeResultDTO compileResult = compileCode(language, codePath, tempDir, containerId);
                    if (!"ACCEPTED".equals(compileResult.getStatus())) {
                        return compileResult;
                    }
                }

                // 运行代码，传递容器ID
                return executeCode(language, codePath, inputPath, tempDir, null, containerId);
            } finally {
                // 清理Docker容器
                dockerService.removeContainer(containerId);
                log.info("代码运行完成，已移除Docker容器: {}", containerId);
            }

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
            
            // 调试输出：打印测试用例信息
            System.out.println("=== 调试信息：测试用例解析结果 ===");
            System.out.println("原始测试用例数据: " + problem.getTestCases());
            System.out.println("解析后测试用例数量: " + testCases.size());
            for (int i = 0; i < testCases.size(); i++) {
                TestCaseDTO testCase = testCases.get(i);
                System.out.println("测试用例 " + (i + 1) + ":");
                System.out.println("  输入: " + testCase.getInput());
                System.out.println("  期望输出: " + testCase.getOutput());
                System.out.println("  分值: " + testCase.getScore());
                System.out.println("  是否为示例: " + testCase.getIsExample());
            }
            System.out.println("=== 调试信息结束 ===");
            
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
            submission.setSubmittedAt(LocalDateTime.now());
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
    
    @Override
    public String getAiCodeReview(CodeSubmission codeSubmission) {
        if (codeSubmission == null || codeSubmission.getId() == null) {
            log.error("代码提交记录为空或ID为空");
            throw new RuntimeException("代码提交记录为空或ID为空");
        }
        
        // 如果只传入了ID，需要查询完整的提交记录
        if (codeSubmission.getCode() == null || codeSubmission.getCode().isEmpty()) {
            CodeSubmission submission = codeSubmissionMapper.selectByPrimaryKey(codeSubmission.getId());
            if (submission == null) {
                log.error("找不到ID为{}的代码提交记录", codeSubmission.getId());
                throw new RuntimeException("找不到对应的代码提交记录");
            }
            codeSubmission = submission;
        }
        
        // 生成唯一的消息ID
        String messageId = codeSubmission.getId().toString();
        
        try {
            // 获取当前用户ID
            Long userId = UserUtils.getCurrentUserByToken().getIdUser();
            
            // 异步发送代码到AI进行评价
            chatService.sendMessageToCoder(
                codeSubmission,
                userId,
                output -> {
                    // 将AI的评价结果添加到消息队列中，供前端轮询获取
                    com.sdumagicode.backend.util.chatUtil.MessageQueueUtil.addMessage(output);
                }
            );
            
            return messageId;
        } catch (Exception e) {
            log.error("获取AI代码评价失败", e);
            throw new RuntimeException("获取AI代码评价失败: " + e.getMessage());
        }
    }

    /**
     * 异步执行评测（仅使用Docker）
     */
    @Async
    protected void executeJudgeAsync(CodeSubmission submission, List<TestCaseDTO> testCases, Problem problem) {
        // 声明Docker容器ID变量
        String containerId = null;
        
        try {
            // 检查Docker是否可用
            if (!dockerService.isDockerAvailable()) {
                log.error("Docker不可用，无法进行代码评测");
                JudgeResultDTO errorResult = JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("系统错误: Docker服务不可用，请确保Docker已正确安装并启动")
                        .build();
                updateSubmissionResult(submission, errorResult);
                return;
            }
            
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("judge_");
            String language = submission.getLanguage();
            String code = submission.getCode();

            // 保存代码到文件
            String filename = getCodeFilename(language);
            Path codePath = tempDir.resolve(filename);
            Files.write(codePath, code.getBytes(StandardCharsets.UTF_8));
            
            // 创建Docker容器
            containerId = dockerService.createContainer(language, tempDir);
            if (containerId == null) {
                log.error("创建Docker容器失败");
                JudgeResultDTO errorResult = JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("系统错误: 创建Docker容器失败")
                        .build();
                updateSubmissionResult(submission, errorResult);
                return;
            }
            
            log.info("使用Docker容器评测代码: {}", containerId);

            // 编译代码
            if (COMPILE_COMMANDS.containsKey(language)) {
                JudgeResultDTO compileResult = compileCode(language, codePath, tempDir, containerId);
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

                // 运行代码，传递容器ID，这样所有测试用例都在同一个容器中运行
                JudgeResultDTO result = executeCode(language, codePath, inputPath, tempDir, problem, containerId);
                
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
        } finally {
            // 清理Docker容器
            if (containerId != null) {
                dockerService.removeContainer(containerId);
                log.info("评测完成，已移除Docker容器: {}", containerId);
            }
        }
    }

    /**
     * 编译代码
     */
    private JudgeResultDTO compileCode(String language, Path codePath, Path tempDir, String containerid) {
        try {
            String[] commandTemplate = COMPILE_COMMANDS.get(language);
            if (commandTemplate == null) {
                return JudgeResultDTO.builder().status("ACCEPTED").build();
            }

            // 检查Docker是否可用
            if (!dockerService.isDockerAvailable()) {
                log.error("Docker不可用，无法进行代码编译");
                return JudgeResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .errorMessage("系统错误: Docker服务不可用，请确保Docker已正确安装并启动")
                        .build();
            }
            
            String containerId = containerid;
            if (containerId == null) {
                log.error("Docker容器ID为空，无法进行编译");
                return JudgeResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .errorMessage("系统错误: Docker容器未创建")
                        .build();
            }
            
            // 使用Docker容器编译
            String[] command;
            String workDir = "/judge";
            
            // 根据语言构建编译命令
            if (language.equals("java")) {
                command = new String[]{"javac", "-encoding", "UTF-8", codePath.getFileName().toString()};
            } else if (language.equals("cpp")) {
                command = new String[]{"g++", "-o", "program", codePath.getFileName().toString(), "-std=c++11"};
            } else {
                // Python不需要编译
                return JudgeResultDTO.builder().status("ACCEPTED").build();
            }
            
            // 在Docker容器中执行编译命令
            Map<String, Object> result = dockerService.executeCompileCommand(containerId, command, workDir);
            
            // 如果执行失败或返回值不为0，则编译失败
            if (result == null) {
                log.error("Docker容器执行编译命令失败");
                return JudgeResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .errorMessage("Docker容器执行编译命令失败")
                        .build();
            }
            
            boolean completed = (boolean) result.get("completed");
            int exitCode = (int) result.get("exitCode");
            String output = (String) result.get("output");
            
            if (!completed || exitCode != 0) {
                return JudgeResultDTO.builder()
                        .status("COMPILATION_ERROR")
                        .errorMessage(output)
                        .build();
            }

            return JudgeResultDTO.builder()
                    .status("ACCEPTED")
                    .build();

        } catch (Exception e) {
            log.error("编译代码失败", e);
            return JudgeResultDTO.builder()
                    .status("COMPILATION_ERROR")
                    .errorMessage("编译错误: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 执行代码（仅使用Docker）
     * @param language 编程语言
     * @param codePath 代码文件路径
     * @param inputPath 输入文件路径
     * @param tempDir 临时目录
     * @param problem 题目信息（可为null）
     * @return 执行结果
     */
    private JudgeResultDTO executeCode(String language, Path codePath, Path inputPath, Path tempDir, Problem problem, String existingContainerId) {
        String containerId = existingContainerId;
        boolean shouldRemoveContainer = false;
        
        try {
            // 设置时间限制
            int timeLimit = problem != null ? problem.getTimeLimit() : 5000; // 默认5秒
            
            // 检查Docker是否可用
            if (!dockerService.isDockerAvailable()) {
                log.error("Docker不可用，无法执行代码");
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("系统错误: Docker服务不可用，请确保Docker已正确安装并启动")
                        .build();
            }
            
            if (containerId == null) {
                // 创建Docker容器
                containerId = dockerService.createContainer(language, tempDir);
                if (containerId == null) {
                    log.error("创建Docker容器失败");
                    return JudgeResultDTO.builder()
                            .status("SYSTEM_ERROR")
                            .errorMessage("系统错误: 创建Docker容器失败")
                            .build();
                } else {
                    log.info("使用Docker容器运行代码: {}", containerId);
                    shouldRemoveContainer = true; // 标记需要在方法结束时移除容器
                }
            } else {
                log.info("使用已存在的Docker容器运行代码: {}", containerId);
            }
            
            try {
                // 使用Docker容器执行
                String[] command;
                String workDir = "/judge";
                
                // 根据语言构建运行命令
                if (language.equals("java")) {
                    command = new String[]{"java", "-Dfile.encoding=UTF-8", "-cp", ".", "Main"};
                } else if (language.equals("cpp")) {
                    command = new String[]{"./program"};
                } else if (language.equals("python")) {
                    command = new String[]{"python", codePath.getFileName().toString()};
                } else {
                    return JudgeResultDTO.builder()
                            .status("SYSTEM_ERROR")
                            .errorMessage("不支持的语言: " + language)
                            .build();
                }
                
                // 在Docker容器中执行运行命令
                String inputFilePath = inputPath.toAbsolutePath().toString();
                Map<String, Object> result = dockerService.executeRunCommand(containerId, command, workDir, inputFilePath, timeLimit);
                
                if (result == null) {
                    log.error("Docker容器执行运行命令失败");
                    return JudgeResultDTO.builder()
                            .status("SYSTEM_ERROR")
                            .errorMessage("Docker容器执行运行命令失败")
                            .build();
                }
                
                boolean completed = (boolean) result.get("completed");
                int exitCode = (int) result.get("exitCode");
                String outputStr = (String) result.get("output");
                // 将Long类型的执行时间转换为int类型
                int executionTime = ((Long) result.get("time")).intValue();
                int memoryUsage = (int) result.get("memory");
                
                if (!completed) {
                    return JudgeResultDTO.builder()
                            .status("TIME_LIMIT_EXCEEDED")
                            .time(executionTime)
                            .memory(memoryUsage)
                            .errorMessage("执行超时")
                            .build();
                }
                
                if (exitCode != 0) {
                    return JudgeResultDTO.builder()
                            .status("RUNTIME_ERROR")
                            .time(executionTime)
                            .memory(memoryUsage)
                            .errorMessage("运行时错误: " + outputStr)
                            .build();
                }

                return JudgeResultDTO.builder()
                        .status("ACCEPTED")
                        .time(executionTime)
                        .memory(memoryUsage)
                        .output(outputStr)
                        .build();
            } finally {
                // 只有当容器是在本方法中创建的时候才清理
                if (containerId != null && shouldRemoveContainer) {
                    dockerService.removeContainer(containerId);
                    log.info("代码执行完成，已移除Docker容器: {}", containerId);
                }
            }
        } catch (Exception e) {
            log.error("执行代码失败", e);
            // 确保在异常情况下也能清理Docker容器
            if (containerId != null && shouldRemoveContainer) {
                try {
                    dockerService.removeContainer(containerId);
                    log.info("异常情况下移除Docker容器: {}", containerId);
                } catch (Exception ex) {
                    log.error("移除Docker容器失败", ex);
                }
            }
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

    private List<TestCaseDTO> parseTestCases(String testCasesJson) {
        if (testCasesJson == null || testCasesJson.isEmpty()) {
            return Collections.emptyList();
        }
    
        try {
            // 先尝试直接解析原始JSON
            List<TestCaseDTO> testCases = objectMapper.readValue(testCasesJson, new TypeReference<List<TestCaseDTO>>() {});
            // 对解析后的测试用例进行二次处理
            return postProcessTestCases(testCases);
        } catch (JsonProcessingException e) {
            log.warn("直接解析JSON失败，尝试修复后再解析: " + e.getMessage());
            try {
                // 解析失败后进行特殊字符处理和引号修复
                String processedJson = processControlChars(testCasesJson);
                log.warn("修复后的JSON: " + processedJson); // 添加日志以便调试
                List<TestCaseDTO> testCases = objectMapper.readValue(processedJson, new TypeReference<List<TestCaseDTO>>() {});
                // 对解析后的测试用例进行二次处理
                return postProcessTestCases(testCases);
            } catch (JsonProcessingException ex) {
                log.error("修复后仍然解析测试用例失败", ex);
                return Collections.emptyList();
            }
        }
    }
    
    /**
     * 对解析后的测试用例进行二次处理
     * 处理包含等号和逗号的输入输出格式
     */
    private List<TestCaseDTO> postProcessTestCases(List<TestCaseDTO> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return testCases;
        }
        
        for (TestCaseDTO testCase : testCases) {
            // 处理输入
            if (testCase.getInput() != null) {
                testCase.setInput(processInputOutput(testCase.getInput()));
            }
            
            // 处理输出
            if (testCase.getOutput() != null) {
                testCase.setOutput(processInputOutput(testCase.getOutput()));
            }
        }
        
        return testCases;
    }
    
    /**
     * 处理输入输出字符串，去除方括号和单引号，处理等号和逗号
     */
    private String processInputOutput(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 去除首尾的方括号
        String processed = value.trim();
        if (processed.startsWith("[") && processed.endsWith("]")) {
            processed = processed.substring(1, processed.length() - 1);
        }
        
        // 去除首尾的单引号
        processed = processed.trim();
        if (processed.startsWith("'") && processed.endsWith("'")) {
            processed = processed.substring(1, processed.length() - 1);
        }
        
        // 检查是否包含等号，如果包含则进行特殊处理
        if (processed.contains("=")) {
            // 处理类似 "s = 'babad'" 或 "dividend = 10, divisor = 3" 的格式
            StringBuilder result = new StringBuilder();
            String[] parts = processed.split(",");
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                
                // 查找等号位置
                int equalIndex = part.indexOf('=');
                if (equalIndex != -1 && equalIndex < part.length() - 1) {
                    // 提取等号后面的值
                    String valueAfterEqual = part.substring(equalIndex + 1).trim();
                    
                    // 去除值的所有单引号
                    valueAfterEqual = valueAfterEqual.replaceAll("^'|'$", ""); // 移除首尾单引号
                    valueAfterEqual = valueAfterEqual.replace("'", ""); // 移除所有剩余的单引号
                    
                    if (result.length() > 0) {
                        result.append("\n");
                    }
                    result.append(valueAfterEqual);
                } else {
                    // 没有等号的部分直接添加
                    if (result.length() > 0) {
                        result.append("\n");
                    }
                    result.append(part);
                }
            }
            
            return result.toString();
        } else {
            // 没有等号，检查是否包含逗号分隔的多个值
            if (processed.contains(",")) {
                // 处理逗号分隔的多个值，将逗号替换为换行符
                StringBuilder result = new StringBuilder();
                String[] parts = processed.split(",");
                
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    
                    // 去除每个部分的单引号（包括首尾和中间的单引号）
                    part = part.replaceAll("^'|'$", ""); // 移除首尾单引号
                    part = part.replace("'", ""); // 移除所有剩余的单引号
                    
                    if (result.length() > 0) {
                        result.append("\n");
                    }
                    result.append(part);
                }
                
                return result.toString();
            } else {
                // 单个值，只需要去除单引号
                return processed;
            }
        }
    }
    
    private String processControlChars(String json) {
        if (json == null) return null;
        
        StringBuilder sb = new StringBuilder(json.length() + 100);
        int state = 0; // 0: 正常状态, 1: 在input值中, 2: 在output值中
        boolean inValue = false; // 是否在值字符串中
        int valueStart = -1; // 值字符串开始位置
        int bracketCount = 0; // 跟踪方括号层级
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            switch (state) {
                case 0: // 正常状态
                    if (c == '"' && i + 6 < json.length() && json.startsWith("input", i + 1)) {
                        // 检测到"input"键
                        sb.append("\"input\"");
                        i += 6; // 跳过"input"
                        continue;
                    } else if (c == '"' && i + 7 < json.length() && json.startsWith("output", i + 1)) {
                        // 检测到"output"键
                        sb.append("\"output\"");
                        i += 7; // 跳过"output"
                        continue;
                    } else if (c == ':' && i > 6) {
                        // 检查前一个token是否是"input"或"output"
                        String prevToken = sb.substring(Math.max(0, sb.length() - 9), sb.length());
                        if (prevToken.endsWith("\"input\"")) {
                            state = 1; // 进入input值处理状态
                        } else if (prevToken.endsWith("\"output\"")) {
                            state = 2; // 进入output值处理状态
                        }
                    }
                    sb.append(c);
                    break;
                    
                case 1: // 在input值中
                case 2: // 在output值中
                    if (c == '"' && !inValue) {
                        // 值字符串开始
                        inValue = true;
                        valueStart = sb.length();
                        bracketCount = 0; // 重置方括号计数
                        sb.append('"'); // 保留边界双引号
                    } else if (c == '"' && inValue) {
                        // 检查是否是值字符串结束的双引号
                        // 通过检查下一个非空白字符来判断
                        boolean isEndQuote = false;
                        for (int j = i + 1; j < json.length(); j++) {
                            char nextChar = json.charAt(j);
                            if (nextChar == ' ' || nextChar == '\t' || nextChar == '\n' || nextChar == '\r') {
                                continue; // 跳过空白字符
                            }
                            if ((nextChar == ',' || nextChar == '}') && bracketCount == 0) {
                                isEndQuote = true;
                            }
                            break;
                        }
                        
                        if (isEndQuote) {
                            // 值字符串结束
                            sb.append('"');
                            state = 0;
                            inValue = false;
                            bracketCount = 0;
                        } else {
                            // 值字符串内部的双引号，改为单引号
                            sb.append('\'');
                        }
                    } else if (c == '[' && inValue) {
                        bracketCount++;
                        sb.append(c);
                    } else if (c == ']' && inValue) {
                        bracketCount--;
                        sb.append(c);
                    } else if ((c == ',' || c == '}') && !inValue) {
                        // 值结束（没有遇到结束双引号的情况）
                        state = 0;
                        sb.append(c);
                    } else if (c == '\\') {
                        // 处理转义字符
                        if (i + 1 < json.length()) {
                            sb.append('\\').append(json.charAt(i + 1));
                            i++;
                        }
                    } else if (c < 32) {
                        // 处理控制字符
                        appendControlChar(sb, c);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        
        return sb.toString();
    }
    
    private void appendControlChar(StringBuilder sb, char c) {
        switch (c) {
            case '\n': sb.append("\\n"); break;
            case '\r': sb.append("\\r"); break;
            case '\t': sb.append("\\t"); break;
            case '\b': sb.append("\\b"); break;
            case '\f': sb.append("\\f"); break;
            default: sb.append(String.format("\\u%04x", (int) c)); break;
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
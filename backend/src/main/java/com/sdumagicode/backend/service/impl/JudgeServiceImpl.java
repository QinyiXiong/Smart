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
     * 初始化方法，在服务启动时输出编译器路径信息
     */
    @javax.annotation.PostConstruct
    public void init() {
        log.info("===== 代码评测服务初始化 =====");
        log.info("Java编译器路径: {}", COMPILER_PATHS.get("java"));
        log.info("C++编译器路径: {}", COMPILER_PATHS.get("cpp"));
        log.info("Python解释器路径: {}", COMPILER_PATHS.get("python"));
        log.info("如需自定义编译器路径，请在启动时添加JVM参数，例如: -Djudge.compiler.cpp=C:\\MinGW\\bin\\g++.exe");
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
    
    // 编译器可执行文件路径映射
    private static final Map<String, String> COMPILER_PATHS = new HashMap<>();
    static {
        // 默认使用系统PATH中的编译器
        COMPILER_PATHS.put("java", "javac");
        COMPILER_PATHS.put("cpp", "g++");
        COMPILER_PATHS.put("python", "python");
        
        // 尝试查找常见的编译器安装路径
        String[] commonCppPaths = {
            "C:\\Program Files\\mingw-w64\\x86_64-8.1.0-posix-seh-rt_v6-rev0\\mingw64\\bin\\g++.exe",
            "C:\\Program Files (x86)\\mingw-w64\\i686-8.1.0-posix-dwarf-rt_v6-rev0\\mingw32\\bin\\g++.exe",
            "C:\\MinGW\\bin\\g++.exe",
            "C:\\msys64\\mingw64\\bin\\g++.exe",
            "C:\\TDM-GCC-64\\bin\\g++.exe"
        };
        
        for (String path : commonCppPaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                COMPILER_PATHS.put("cpp", path);
                log.info("找到C++编译器路径: {}", path);
                break;
            }
        }
        
        // 从系统环境变量中获取自定义编译器路径
        String customCppPath = System.getProperty("judge.compiler.cpp");
        if (customCppPath != null && !customCppPath.isEmpty()) {
            File file = new File(customCppPath);
            if (file.exists() && file.canExecute()) {
                COMPILER_PATHS.put("cpp", customCppPath);
                log.info("使用自定义C++编译器路径: {}", customCppPath);
            } else {
                log.warn("自定义C++编译器路径无效: {}", customCppPath);
            }
        }
    }
    
    /**
     * 设置编译器路径
     * @param language 语言
     * @param path 编译器路径
     * @return 是否设置成功
     */
    public static boolean setCompilerPath(String language, String path) {
        if (language == null || path == null || path.isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        if (file.exists() && file.canExecute()) {
            COMPILER_PATHS.put(language, path);
            log.info("设置{}编译器路径: {}", language, path);
            return true;
        } else {
            log.warn("无效的{}编译器路径: {}", language, path);
            return false;
        }
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

            // 检查Docker是否可用
            boolean useDocker = dockerService.isDockerAvailable();
            String containerId = null;
            
            if (useDocker) {
                // 创建Docker容器
                containerId = dockerService.createContainer(language, tempDir);
                if (containerId == null) {
                    log.warn("创建Docker容器失败，将使用本地环境运行");
                    useDocker = false;
                } else {
                    log.info("使用Docker容器运行代码: {}", containerId);
                }
            }
            
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
                if (useDocker && containerId != null) {
                    dockerService.removeContainer(containerId);
                }
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
     * 异步执行评测
     */
    @Async
    protected void executeJudgeAsync(CodeSubmission submission, List<TestCaseDTO> testCases, Problem problem) {
        // 声明Docker容器ID变量
        String containerId = null;
        boolean useDocker = false;
        
        try {
            // 创建临时目录
            Path tempDir = Files.createTempDirectory("judge_");
            String language = submission.getLanguage();
            String code = submission.getCode();

            // 保存代码到文件
            String filename = getCodeFilename(language);
            Path codePath = tempDir.resolve(filename);
            Files.write(codePath, code.getBytes(StandardCharsets.UTF_8));
            
            // 检查Docker是否可用
            useDocker = dockerService.isDockerAvailable();
            
            if (useDocker) {
                // 创建Docker容器
                containerId = dockerService.createContainer(language, tempDir);
                if (containerId == null) {
                    log.warn("创建Docker容器失败，将使用本地环境评测");
                    useDocker = false;
                } else {
                    log.info("使用Docker容器评测代码: {}", containerId);
                }
            }

            // 编译代码（如果需要）
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
            if (useDocker && containerId != null) {
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
            boolean useDocker = dockerService.isDockerAvailable();
            String containerId = containerid;
            
            // 注意：containerId应该由调用方传入，这里不再创建新容器
            // 如果调用方已经创建了容器，则使用Docker环境编译
            
            if (!useDocker) {
                // 使用本地环境编译（原有逻辑）
                // 获取编译器路径
                String compilerPath = COMPILER_PATHS.get(language);
                if (compilerPath == null) {
                    log.error("找不到{}编译器路径", language);
                    return JudgeResultDTO.builder()
                            .status("COMPILATION_ERROR")
                            .errorMessage("系统错误: 找不到" + language + "编译器，请确保已安装相应编译器并配置环境变量，或使用-Djudge.compiler." + language + "参数指定编译器路径")
                            .build();
                }
                
                // 检查编译器文件是否存在
                File compilerFile = new File(compilerPath);
                if (!compilerFile.exists()) {
                    log.error("编译器文件不存在: {}", compilerPath);
                    return JudgeResultDTO.builder()
                            .status("COMPILATION_ERROR")
                            .errorMessage("编译错误: 找不到编译器文件 " + compilerPath + "，请检查编译器安装路径")
                            .build();
                }

                String[] command = new String[commandTemplate.length];
                // 替换第一个元素为实际编译器路径
                command[0] = compilerPath;
                for (int i = 1; i < commandTemplate.length; i++) {
                    command[i] = commandTemplate[i]
                            .replace("{filename}", codePath.toString())
                            .replace("{output}", tempDir.resolve("program").toString())
                            .replace("{dir}", tempDir.toString());
                }

                // 为Java运行设置UTF-8编码和显式类路径
                if (language.equals("java")) {
                    command = new String[]{COMPILER_PATHS.get("java"), "-encoding", "UTF-8", "-cp", ".", codePath.toString()};
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
            } else {
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
            }

            return JudgeResultDTO.builder()
                    .status("ACCEPTED")
                    .build();

        } catch (Exception e) {
            log.error("编译代码失败", e);
            String errorMsg = e.getMessage();
            
            // 检测是否为CreateProcess错误（找不到编译器的常见错误）
            if (errorMsg != null && errorMsg.contains("CreateProcess")) {
                return JudgeResultDTO.builder()
                    .status("COMPILATION_ERROR")
                    .errorMessage("编译错误: 无法启动编译器进程，请确保已正确安装C++编译器(g++)并添加到系统PATH环境变量中，" +
                                "或使用-Djudge.compiler.cpp=<编译器完整路径> 参数指定编译器路径。\n" +
                                "原始错误: " + errorMsg)
                    .build();
            }
            
            return JudgeResultDTO.builder()
                    .status("COMPILATION_ERROR")
                    .errorMessage("编译错误: " + errorMsg)
                    .build();
        }
    }

    /**
     * 执行代码
     * @param language 编程语言
     * @param codePath 代码文件路径
     * @param inputPath 输入文件路径
     * @param tempDir 临时目录
     * @param problem 题目信息（可为null）
     * @return 执行结果
     */
    private JudgeResultDTO executeCode(String language, Path codePath, Path inputPath, Path tempDir, Problem problem, String existingContainerId) {
        String containerId = existingContainerId;
        boolean useDocker = false;
        boolean shouldRemoveContainer = false;
        
        try {
            String[] commandTemplate = RUN_COMMANDS.get(language);
            if (commandTemplate == null) {
                return JudgeResultDTO.builder()
                        .status("SYSTEM_ERROR")
                        .errorMessage("不支持的语言: " + language)
                        .build();
            }

            // 设置时间限制
            int timeLimit = problem != null ? problem.getTimeLimit() : 5000; // 默认5秒
            
            // 检查Docker是否可用，并且是否已经有容器ID
            useDocker = dockerService.isDockerAvailable();
            
            if (useDocker && containerId == null) {
                // 创建Docker容器（仅当没有传入现有容器ID时）
                containerId = dockerService.createContainer(language, tempDir);
                if (containerId == null) {
                    log.warn("创建Docker容器失败，将使用本地环境运行");
                    useDocker = false;
                } else {
                    log.info("使用Docker容器运行代码: {}", containerId);
                    shouldRemoveContainer = true; // 标记需要在方法结束时移除容器
                }
            } else if (containerId != null) {
                log.info("使用已存在的Docker容器运行代码: {}", containerId);
            }
            
            try {
                if (!useDocker) {
                    // 使用本地环境运行（原有逻辑）
                    // 获取运行程序路径
                    String runnerPath = COMPILER_PATHS.get(language);
                    if (runnerPath == null) {
                        log.error("找不到{}运行环境", language);
                        return JudgeResultDTO.builder()
                                .status("SYSTEM_ERROR")
                                .errorMessage("系统错误: 找不到" + language + "运行环境，请确保已安装相应运行环境并配置环境变量，或使用-Djudge.compiler." + language + "参数指定路径")
                                .build();
                    }
                    
                    // 检查运行程序文件是否存在
                    File runnerFile = new File(runnerPath);
                    if (!runnerFile.exists()) {
                        log.error("运行程序文件不存在: {}", runnerPath);
                        return JudgeResultDTO.builder()
                                .status("SYSTEM_ERROR")
                                .errorMessage("系统错误: 找不到运行程序文件 " + runnerPath + "，请检查安装路径")
                                .build();
                    }
                    
                    // 记录运行命令信息，便于调试
                    log.info("执行{}代码，使用运行环境: {}", language, runnerPath);

                    String[] command = new String[commandTemplate.length];
                    // 对于需要解释器的语言（如Python），替换第一个元素为实际路径
                    if (language.equals("python")) {
                        command[0] = runnerPath;
                        for (int i = 1; i < commandTemplate.length; i++) {
                            command[i] = commandTemplate[i]
                                    .replace("{filename}", codePath.toString())
                                    .replace("{output}", tempDir.resolve("program").toString())
                                    .replace("{dir}", tempDir.toString());
                        }
                    } else {
                        // 对于编译型语言，保持原有命令结构
                        for (int i = 0; i < commandTemplate.length; i++) {
                            command[i] = commandTemplate[i]
                                    .replace("{filename}", codePath.toString())
                                    .replace("{output}", tempDir.resolve("program").toString())
                                    .replace("{dir}", tempDir.toString());
                        }
                    }

                    // 为Java运行设置UTF-8编码和显式类路径
                    if (language.equals("java")) {
                        command = new String[]{runnerPath, "-Dfile.encoding=UTF-8", "-cp", tempDir.toString(), "Main"};
                    }

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.directory(tempDir.toFile());
                    pb.redirectInput(inputPath.toFile());
                    pb.redirectErrorStream(true);

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
                    String outputStr = output.toString();
                    int exitCode = process.exitValue();
                    
                    if (exitCode != 0) {
                        return JudgeResultDTO.builder()
                                .status("RUNTIME_ERROR")
                                .time(executionTime)
                                .memory(50) // 默认值
                                .errorMessage("运行时错误: " + outputStr)
                                .build();
                    }

                    return JudgeResultDTO.builder()
                            .status("ACCEPTED")
                            .time(executionTime)
                            .memory(50) // 默认值
                            .output(outputStr)
                            .build();
                } else {
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
                }
            } finally {
                // 只有当容器是在本方法中创建的时候才清理
                if (useDocker && containerId != null && shouldRemoveContainer) {
                    dockerService.removeContainer(containerId);
                    log.info("代码执行完成，已移除Docker容器: {}", containerId);
                }
            }
        } catch (Exception e) {
            log.error("执行代码失败", e);
            // 确保在异常情况下也能清理Docker容器
            if (useDocker && containerId != null) {
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
            return objectMapper.readValue(testCasesJson, new TypeReference<List<TestCaseDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("直接解析JSON失败，尝试修复后再解析: " + e.getMessage());
            try {
                // 解析失败后进行特殊字符处理和引号修复
                String processedJson = processControlChars(testCasesJson);
                log.warn("修复后的JSON: " + processedJson); // 添加日志以便调试
                return objectMapper.readValue(processedJson, new TypeReference<List<TestCaseDTO>>() {});
            } catch (JsonProcessingException ex) {
                log.error("修复后仍然解析测试用例失败", ex);
                return Collections.emptyList();
            }
        }
    }
    
    private String processControlChars(String json) {
        if (json == null) return null;
        
        StringBuilder sb = new StringBuilder(json.length() + 100);
        int state = 0; // 0: 正常状态, 1: 在input值中
        boolean inValue = false; // 是否在input值字符串中
        int valueStart = -1; // 值字符串开始位置
        int sum=0;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            switch (state) {
                case 0: // 正常状态
                    if (c == '"' && i + 6 < json.length() && json.startsWith("input", i + 1)) {
                        // 检测到"input"键
                        sb.append("\"input\"");
                        sum=0;
                        i += 6; // 跳过"input"
                        continue;
                    } else if (c == ':' && i > 6) {
                        // 检查前一个token是否是"input"
                        String prevToken = sb.substring(Math.max(0, sb.length() - 8), sb.length());
                        if (prevToken.endsWith("\"input\"")) {
                            state = 1; // 进入input值处理状态
                        }
                    }
                    sb.append(c);
                    break;
                    
                case 1: // 在input值中
                    if (c == '"' && !inValue) {
                        // 值字符串开始
                        inValue = true;
                        valueStart = sb.length();
                        sb.append('"'); // 保留边界双引号
                    } else if (c == '"' && inValue && sum<2) {
                        // 值字符串内部的双引号，改为单引号
                        ++sum;
                        sb.append('\'');
                    }else if (c == ',' || c == '}') {
                        // 值结束
                        state = 0;
                        inValue = false;
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
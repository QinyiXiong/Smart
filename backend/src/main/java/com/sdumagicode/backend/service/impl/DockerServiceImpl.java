package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.service.DockerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Docker容器服务实现类
 */
@Service
@Slf4j
public class DockerServiceImpl implements DockerService {

    @Value("${judge.docker.enabled:false}")
    private boolean dockerEnabled;

    @Value("${judge.docker.image.java:openjdk:11-jdk}")
    private String javaImage;

    @Value("${judge.docker.image.cpp:gcc:latest}")
    private String cppImage;

    @Value("${judge.docker.image.python:python:3.9}")
    private String pythonImage;

    @Value("${judge.docker.memory:512m}")
    private String memoryLimit;

    @Value("${judge.docker.cpu:1.0}")
    private String cpuLimit;

    @Override
    public String createContainer(String language, Path tempDir) {
        if (!dockerEnabled) {
            log.warn("Docker功能未启用，将使用本地环境执行代码");
            return null;
        }

        try {
            // 根据语言选择镜像
            String image = getDockerImage(language);
            String containerId = UUID.randomUUID().toString().substring(0, 8);
            String hostPath = tempDir.toAbsolutePath().toString();
            String containerPath = "/judge";

            // 创建并启动容器
            String[] command = {
                "docker", "run", "-d",
                "--name", containerId,
                "--memory", memoryLimit,
                "--cpus", cpuLimit,
                "-v", hostPath + ":" + containerPath,
                "--network", "none", // 禁止网络访问
                "-w", containerPath, // 设置工作目录
                image,
                "tail", "-f", "/dev/null" // 保持容器运行
            };

            Process process = Runtime.getRuntime().exec(command);
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);

            if (!completed || process.exitValue() != 0) {
                log.error("创建Docker容器失败: {}", readProcessOutput(process));
                return null;
            }

            log.info("成功创建Docker容器: {}, 语言: {}", containerId, language);
            return containerId;
        } catch (Exception e) {
            log.error("创建Docker容器异常", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> executeCompileCommand(String containerId, String[] command, String workDir) {
        if (!dockerEnabled || containerId == null) {
            return null; // 如果Docker未启用，返回null表示使用本地环境
        }

        try {
            // 构建在容器中执行的命令
            String[] dockerCommand = buildDockerExecCommand(containerId, command, workDir);

            Process process = Runtime.getRuntime().exec(dockerCommand);
            boolean completed = process.waitFor(30, TimeUnit.SECONDS); // 编译给30秒超时

            Map<String, Object> result = new HashMap<>();
            result.put("completed", completed);
            result.put("exitCode", completed ? process.exitValue() : -1);
            result.put("output", readProcessOutput(process));

            return result;
        } catch (Exception e) {
            log.error("在Docker容器中执行编译命令异常", e);
            Map<String, Object> result = new HashMap<>();
            result.put("completed", false);
            result.put("exitCode", -1);
            result.put("output", "执行异常: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> executeRunCommand(String containerId, String[] command, String workDir,
                                              String inputFile, int timeLimit) {
        if (!dockerEnabled || containerId == null) {
            return null; // 如果Docker未启用，返回null表示使用本地环境
        }

        try {
            // 构建在容器中执行的命令
            String[] dockerCommand = buildDockerExecCommand(containerId, command, workDir);

            // 开始计时
            long startTime = System.currentTimeMillis();

            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(dockerCommand);
            if (inputFile != null && !inputFile.isEmpty()) {
                // 如果有输入文件，将其内容作为标准输入
                pb.redirectInput(new File(inputFile));
            }
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean completed = process.waitFor(timeLimit, TimeUnit.MILLISECONDS);
            long endTime = System.currentTimeMillis();
            int executionTime = (int) (endTime - startTime);

            // 获取内存使用情况（这里只是一个估计值，实际应该使用Docker stats API获取更准确的值）
            int memoryUsage = 50; // 默认值
            try {
                String[] statsCommand = {"docker", "stats", containerId, "--no-stream", "--format", "{{.MemUsage}}"};
                Process statsProcess = Runtime.getRuntime().exec(statsCommand);
                statsProcess.waitFor(2, TimeUnit.SECONDS);
                String statsOutput = readProcessOutput(statsProcess);
                // 解析内存使用（格式通常是 "123.4MiB / 512MiB"）
                if (statsOutput.contains("MiB")) {
                    String memUsage = statsOutput.split("/")[0].trim();
                    memUsage = memUsage.replace("MiB", "").trim();
                    try {
                        memoryUsage = (int) Double.parseDouble(memUsage);
                    } catch (NumberFormatException e) {
                        log.warn("解析内存使用失败: {}", memUsage);
                    }
                }
            } catch (Exception e) {
                log.warn("获取容器内存使用异常", e);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("completed", completed);
            result.put("exitCode", completed ? process.exitValue() : -1);
            result.put("output", readProcessOutput(process));
            result.put("time", executionTime);
            result.put("memory", memoryUsage);

            return result;
        } catch (Exception e) {
            log.error("在Docker容器中执行运行命令异常", e);
            Map<String, Object> result = new HashMap<>();
            result.put("completed", false);
            result.put("exitCode", -1);
            result.put("output", "执行异常: " + e.getMessage());
            result.put("time", 0);
            result.put("memory", 0);
            return result;
        }
    }

    @Override
    public void removeContainer(String containerId) {
        if (!dockerEnabled || containerId == null) {
            return;
        }

        try {
            // 停止容器
            String[] stopCommand = {"docker", "stop", containerId};
            Process stopProcess = Runtime.getRuntime().exec(stopCommand);
            stopProcess.waitFor(5, TimeUnit.SECONDS);

            // 删除容器
            String[] rmCommand = {"docker", "rm", "-f", containerId};
            Process rmProcess = Runtime.getRuntime().exec(rmCommand);
            rmProcess.waitFor(5, TimeUnit.SECONDS);

            log.info("已删除Docker容器: {}", containerId);
        } catch (Exception e) {
            log.error("删除Docker容器异常: {}", containerId, e);
        }
    }

    @Override
    public boolean isDockerAvailable() {
        if (!dockerEnabled) {
            return false;
        }

        try {
            Process process = Runtime.getRuntime().exec("docker --version");
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.error("检查Docker可用性异常", e);
            return false;
        }
    }

    /**
     * 根据语言获取对应的Docker镜像
     */
    private String getDockerImage(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return javaImage;
            case "cpp":
                return cppImage;
            case "python":
                return pythonImage;
            default:
                return pythonImage; // 默认使用Python镜像
        }
    }

    /**
     * 构建在Docker容器中执行的命令
     */
    private String[] buildDockerExecCommand(String containerId, String[] command, String workDir) {
        // 基本的docker exec命令
        String[] baseCommand = {"docker", "exec", "-i"};
        
        // 如果指定了工作目录
        if (workDir != null && !workDir.isEmpty()) {
            baseCommand = new String[]{"docker", "exec", "-i", "-w", workDir};
        }
        
        // 完整命令 = docker exec -i [-w workDir] containerId command...
        String[] fullCommand = new String[baseCommand.length + 1 + command.length];
        System.arraycopy(baseCommand, 0, fullCommand, 0, baseCommand.length);
        fullCommand[baseCommand.length] = containerId;
        System.arraycopy(command, 0, fullCommand, baseCommand.length + 1, command.length);
        
        return fullCommand;
    }

    /**
     * 读取进程的输出
     */
    private String readProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (IOException e) {
            log.error("读取进程输出异常", e);
            return "读取输出异常: " + e.getMessage();
        }
    }
}
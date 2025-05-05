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
 * Docker服务实现类
 */
@Service
@Slf4j
public class DockerServiceImpl implements DockerService {

    @Value("${judge.docker.image:judge-env}")
    private String judgeImage;

    @Value("${judge.docker.memory-limit:512m}")
    private String memoryLimit;

    @Value("${judge.docker.cpu-limit:1.0}")
    private String cpuLimit;

    @Value("${judge.docker.network:none}")
    private String network;

    @Value("${judge.docker.timeout:10000}")
    private int dockerTimeout;

    @Override
    public boolean isDockerAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "version");
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0) {
                log.info("Docker可用");
                return true;
            }
            log.warn("Docker不可用");
            return false;
        } catch (Exception e) {
            log.error("检查Docker可用性失败", e);
            return false;
        }
    }

    @Override
    public String createContainer(String language, Path codePath) {
        try {
            // 生成容器ID
            String containerId = "judge-" + UUID.randomUUID().toString().substring(0, 8);
            
            // 创建容器
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "run",
                "--name", containerId,
                "--memory", memoryLimit,
                "--cpus", cpuLimit,
                "--network", network,
                "--rm",
                "-d",
                "-v", codePath.toAbsolutePath() + ":/judge",
                judgeImage,
                "sleep", "30" // 容器启动后休眠等待命令执行
            );
            
            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            
            if (!completed || process.exitValue() != 0) {
                log.error("创建Docker容器失败");
                return null;
            }
            
            log.info("创建Docker容器成功: {}", containerId);
            return containerId;
        } catch (Exception e) {
            log.error("创建Docker容器异常", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> executeCompileCommand(String containerId, String[] command, String workDir) {
        Map<String, Object> result = new HashMap<>();
        result.put("completed", false);
        result.put("exitCode", -1);
        result.put("output", "");
        
        try {
            // 参数检查
            if (containerId == null || command == null || workDir == null) {
                throw new IllegalArgumentException("容器ID、命令或工作目录不能为空");
            }
            
            // 构建Docker exec命令
            String[] dockerCommand = buildDockerExecCommand(containerId, command, workDir);
            if (dockerCommand == null) {
                throw new IllegalStateException("构建Docker命令失败");
            }
            
            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(dockerCommand);
            Process process = pb.start();
            
            // 等待命令执行完成
            boolean completed = process.waitFor(dockerTimeout, TimeUnit.MILLISECONDS);
            result.put("completed", completed);
            
            if (completed) {
                int exitCode = process.exitValue();
                result.put("exitCode", exitCode);
                
                // 读取输出
                String output = readProcessOutput(process);
                result.put("output", output);
                
                log.info("Docker容器编译命令执行完成: exitCode={}", exitCode);
            } else {
                process.destroyForcibly();
                log.warn("Docker容器编译命令执行超时");
                result.put("output", "编译超时");
            }
            
            return result;
        } catch (Exception e) {
            log.error("执行Docker容器编译命令异常", e);
            result.put("output", "执行编译命令异常: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> executeRunCommand(String containerId, String[] command, String workDir, String inputFilePath, Integer timeLimit) {
        Map<String, Object> result = new HashMap<>();
        result.put("completed", false);
        result.put("exitCode", -1);
        result.put("output", "");
        result.put("error", "");
        result.put("time", 0L);
        result.put("memory", 0);
        
        try {
            // 构建Docker exec命令
            String[] dockerCommand = buildDockerExecCommand(containerId, command, workDir);
            
            // 执行命令
            ProcessBuilder pb = new ProcessBuilder(dockerCommand);
            Process process = pb.start();
            
            // 如果有输入文件，将内容写入进程的标准输入
            if (inputFilePath != null && !inputFilePath.isEmpty()) {
                try (OutputStream stdin = process.getOutputStream();
                     BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdin.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                        stdin.flush();
                    }
                }
            }
            
            // 设置超时时间（默认为10秒，可由timeLimit参数覆盖）
            int timeout = timeLimit != null ? timeLimit : dockerTimeout;
            long startTime = System.currentTimeMillis();
            boolean completed = process.waitFor(timeout, TimeUnit.MILLISECONDS);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            result.put("completed", completed);
            result.put("time", executionTime); // 确保executionTime作为Long类型存储
            
            if (completed) {
                int exitCode = process.exitValue();
                result.put("exitCode", exitCode);
                
                // 读取标准输出和错误输出
                String output = readProcessOutput(process);
                String error = readProcessError(process);
                result.put("output", output);
                result.put("error", error);
                
                // 获取内存使用情况（这里只是一个估计值，实际应该通过Docker stats获取）
                result.put("memory", 0); // 暂时不实现内存统计
                
                log.info("Docker容器运行命令执行完成: exitCode={}, time={}ms", exitCode, executionTime);
            } else {
                process.destroyForcibly();
                log.warn("Docker容器运行命令执行超时");
                result.put("error", "运行超时");
            }
            
            return result;
        } catch (Exception e) {
            log.error("执行Docker容器运行命令异常", e);
            result.put("error", "执行运行命令异常: " + e.getMessage());
            return result;
        }
    }

    @Override
    public boolean removeContainer(String containerId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "rm", "-f", containerId);
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            
            if (completed && process.exitValue() == 0) {
                log.info("移除Docker容器成功: {}", containerId);
                return true;
            }
            
            log.warn("移除Docker容器失败: {}", containerId);
            return false;
        } catch (Exception e) {
            log.error("移除Docker容器异常", e);
            return false;
        }
    }
    
    /**
     * 构建Docker exec命令
     * 使用bash -c命令来设置工作目录，避免使用-w参数
     */
    private String[] buildDockerExecCommand(String containerId, String[] command, String workDir) {
        // 基础Docker exec命令前缀
        String[] dockerExecPrefix = {"docker", "exec", "-i", containerId};
        
        // 使用bash -c来执行命令并设置工作目录
        // 构建命令字符串：cd 到指定目录，然后执行原始命令
        StringBuilder commandStr = new StringBuilder();
        commandStr.append("cd ").append(workDir).append(" && ");
        
        // 添加原始命令到命令字符串
        for (int i = 0; i < command.length; i++) {
            commandStr.append(command[i]);
            if (i < command.length - 1) {
                commandStr.append(" ");
            }
        }
        
        // 最终命令：docker exec -i containerId bash -c "cd /path && original command"
        String[] fullCommand = new String[dockerExecPrefix.length + 3];
        System.arraycopy(dockerExecPrefix, 0, fullCommand, 0, dockerExecPrefix.length);
        fullCommand[dockerExecPrefix.length] = "bash";
        fullCommand[dockerExecPrefix.length + 1] = "-c";
        fullCommand[dockerExecPrefix.length + 2] = commandStr.toString();
        
        return fullCommand;
    }
    
    /**
     * 读取进程的标准输出
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
    
    /**
     * 读取进程的错误输出
     */
    private String readProcessError(Process process) throws IOException {
        StringBuilder error = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                error.append(line).append("\n");
            }
        }
        return error.toString();
    }
}
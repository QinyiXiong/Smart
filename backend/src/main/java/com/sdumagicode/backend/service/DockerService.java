package com.sdumagicode.backend.service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Docker服务接口，用于管理代码评测的Docker容器
 */
public interface DockerService {
    
    /**
     * 检查Docker是否可用
     * @return 是否可用
     */
    boolean isDockerAvailable();
    
    /**
     * 创建Docker容器
     * @param language 编程语言
     * @param codePath 代码路径
     * @return 容器ID
     */
    String createContainer(String language, Path codePath);
    
    /**
     * 在容器中执行编译命令
     * @param containerId 容器ID
     * @param command 编译命令
     * @param workDir 工作目录
     * @return 执行结果
     */
    Map<String, Object> executeCompileCommand(String containerId, String[] command, String workDir);
    
    /**
     * 在容器中执行运行命令
     * @param containerId 容器ID
     * @param command 运行命令
     * @param workDir 工作目录
     * @param inputFilePath 输入文件路径
     * @param timeLimit 时间限制(毫秒)
     * @return 执行结果
     */
    Map<String, Object> executeRunCommand(String containerId, String[] command, String workDir, String inputFilePath, Integer timeLimit);
    
    /**
     * 移除容器
     * @param containerId 容器ID
     * @return 是否成功
     */
    boolean removeContainer(String containerId);
}
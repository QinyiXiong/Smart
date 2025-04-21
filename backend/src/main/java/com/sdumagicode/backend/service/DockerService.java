package com.sdumagicode.backend.service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Docker容器服务接口
 * 用于管理代码评测的Docker容器
 */
public interface DockerService {
    
    /**
     * 创建并启动一个用于代码评测的容器
     * 
     * @param language 编程语言
     * @param tempDir 临时目录路径（用于挂载到容器中）
     * @return 容器ID
     */
    String createContainer(String language, Path tempDir);
    
    /**
     * 在容器中执行编译命令
     * 
     * @param containerId 容器ID
     * @param command 编译命令
     * @param workDir 工作目录
     * @return 编译结果（包含退出码和输出信息）
     */
    Map<String, Object> executeCompileCommand(String containerId, String[] command, String workDir);
    
    /**
     * 在容器中执行代码
     * 
     * @param containerId 容器ID
     * @param command 运行命令
     * @param workDir 工作目录
     * @param inputFile 输入文件路径
     * @param timeLimit 时间限制（毫秒）
     * @return 执行结果（包含退出码、输出信息、执行时间和内存使用）
     */
    Map<String, Object> executeRunCommand(String containerId, String[] command, String workDir, 
                                        String inputFile, int timeLimit);
    
    /**
     * 停止并删除容器
     * 
     * @param containerId 容器ID
     */
    void removeContainer(String containerId);
    
    /**
     * 检查Docker服务是否可用
     * 
     * @return 是否可用
     */
    boolean isDockerAvailable();
}
package com.sdumagicode.backend.util.embeddingUtil;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class KnowledgeRecord {
    // 必选字段
    private String recordId;      // 知识记录唯一ID（建议UUID）
    private String fileId;        // 关联的原始文件ID
    private String chunkText;     // 文本分块内容（核心向量化目标）
    private Integer chunkIndex;   // 分块序号（用于重组文件）
    
    // 可选元数据字段
    private String fileName;      // 原始文件名


}

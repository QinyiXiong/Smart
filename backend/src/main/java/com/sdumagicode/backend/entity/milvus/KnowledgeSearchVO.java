package com.sdumagicode.backend.entity.milvus;

import lombok.Data;

import java.util.Map;

@Data
public class KnowledgeSearchVO {
    // 核心检索结果
    private String chunkId;          // 文本块唯一ID（对应Milvus主键）
    private String fileId;           // 关联的原始文件ID
    private String chunkText;        // 匹配的文本内容
    private Float relevanceScore;    // 相似度分数（L2距离）
    

    
    // 业务扩展字段

    // 分数格式化（转换为0-1的相似度）
    public Double getFormattedScore() {
        return 1 / (1 + Math.exp(relevanceScore)); // Sigmoid归一化
    }
    

}
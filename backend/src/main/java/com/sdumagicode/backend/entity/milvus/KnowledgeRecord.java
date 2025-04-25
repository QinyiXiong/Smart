package com.sdumagicode.backend.entity.milvus;

import com.sdumagicode.backend.util.SnowflakeIdGenerator;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class KnowledgeRecord {

    // 必选字段
    private Long recordId;      // 知识记录唯一ID（建议UUID）
    private String fileId;        // 关联的原始文件ID
    @Transient
    private String chunkText;     // 文本分块内容（核心向量化目标）
    private Integer chunkIndex;   // 分块序号（用于重组文件）
    
    // 可选元数据字段
    private String fileName;      // 原始文件名

    //用于多线程时记录embedding
    @Transient
    private List<Float> embedding;
//    private List<List<Float>> embedding;


    public void generateRecordId() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        this.recordId = generator.nextId();
    }

}

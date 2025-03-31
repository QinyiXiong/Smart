package com.sdumagicode.backend.entity.chat;

import com.baomidou.mybatisplus.annotation.TableField;
import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import com.sdumagicode.backend.entity.milvus.MilvusFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("interviewer")
public class Interviewer {

    @Id
    private String interviewerId;

    private Long userId;

    private String knowledgeBaseId;

    private final String promptTemplate = "test";

    private String customPrompt;

    private List<Map.Entry<AiSettings, Integer>> settingsList;
}

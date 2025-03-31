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
    private Long interviewerId;


    private Long userId;


    private MilvusDatabase database;


    private String promptTemplate;

    // 复杂类型需要特殊处理，MyBatis-Plus默认不支持直接映射
    // 方案：使用@TableField(exist = false)标注为非表字段，自行处理
    @TableField(exist = false)
    private List<Map.Entry<AiSettings, Integer>> settingsList;
}

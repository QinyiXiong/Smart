package com.sdumagicode.backend.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("interviewer")
public class Interviewer {

    @TableId(type = IdType.AUTO)
    private Long interviewerId;

    @TableField("user_id")
    private Long userId;

    @TableField("data_base_id")
    private String dataBaseId;

    @TableField("prompt_template")
    private String promptTemplate;

    // 复杂类型需要特殊处理，MyBatis-Plus默认不支持直接映射
    // 方案：使用@TableField(exist = false)标注为非表字段，自行处理
    @TableField(exist = false)
    private List<Map.Entry<AiSettings, Integer>> settingsList;
}

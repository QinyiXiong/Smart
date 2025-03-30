package com.sdumagicode.backend.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//关联中间表
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("interviewer_ai_settings")
public class InterviewerAiSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("interviewer_id")
    private Long interviewerId;

    @TableField("ai_setting_id")
    private Integer aiSettingId;

    @TableField("setting_value")
    private Integer settingValue;
}

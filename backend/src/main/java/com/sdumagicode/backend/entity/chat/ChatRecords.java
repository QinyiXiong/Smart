package com.sdumagicode.backend.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("interview_chat_records")
public class ChatRecords {
    @TableId(value = "chat_id", type = IdType.AUTO)
    private Long chatId;

    @TableField("user_id")  // 替换为 MyBatis-Plus 的注解
    private Long userId;

    @TableField("interviewer_id")
    private String interviewerId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("topic")
    private String topic;
}

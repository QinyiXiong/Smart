package com.sdumagicode.backend.entity.chat;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_settings") // 指定表名
public class AiSettings {

    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "id") // 主键自增
    private Integer id;

    @Column(name = "setting_name") // 字段名映射
    private String settingName;

    @Column(name = "description")
    private String description;
}

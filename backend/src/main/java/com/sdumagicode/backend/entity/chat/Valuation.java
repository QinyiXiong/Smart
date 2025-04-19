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
@TableName("interview_valuation") // 指定表名
public class Valuation {
    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "id") // 主键自增
    private Integer id;

    @Column(name = "valuation_name") // 字段名映射
    private String valuationName;

    @Column(name = "valuation_description")
    private String valuationDescription;
}

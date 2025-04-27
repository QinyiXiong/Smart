package com.sdumagicode.backend.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("interview_valuation")
public class ValuationStandard {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("valuation_name")
    private String valuationName;

    @TableField("valuation_description")
    private String valuationDescription;
}
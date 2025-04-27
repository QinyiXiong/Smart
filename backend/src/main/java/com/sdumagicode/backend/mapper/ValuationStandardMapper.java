package com.sdumagicode.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sdumagicode.backend.entity.chat.ValuationStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ValuationStandardMapper extends BaseMapper<ValuationStandard> {
    @Select("SELECT id, valuation_name as valuationName, valuation_description as valuationDescription " +
            "FROM interview_valuation " +
            "ORDER BY id ASC")
    List<ValuationStandard> selectValuationList();
}
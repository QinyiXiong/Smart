package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.entity.chat.AiSettings;
import com.sdumagicode.backend.entity.chat.Valuation;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@org.apache.ibatis.annotations.Mapper
public interface ValuationMapper extends Mapper<Valuation> {
    @Select("select id, valuation_description as valuationDescription, valuation_name as valuationName from interview_valuation")
    public List<Valuation> selectAll();
}

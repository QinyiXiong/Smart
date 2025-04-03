package com.sdumagicode.backend.mapper;


import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.sdumagicode.backend.entity.chat.AiSettings;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

@org.apache.ibatis.annotations.Mapper
public interface AiSettingMapper extends Mapper<AiSettings> {
    @Select("select id, description, setting_name as settingName from ai_settings")
    public List<AiSettings> selectAll();
}

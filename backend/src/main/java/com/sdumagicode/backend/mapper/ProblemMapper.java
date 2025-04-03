package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.dto.ProblemDTO;
import com.sdumagicode.backend.entity.Problem;
import com.sdumagicode.backend.core.mapper.Mapper;
import java.util.List;
import java.util.Map;

public interface ProblemMapper extends Mapper<Problem> {
    /**
     * 查询题目列表数据
     *
     * @return
     */
    List<ProblemDTO> selectProblems();

    /**
     * 查询所有标签
     *
     * @return 所有标签列表
     */
    List<String> selectAllTags();

    /**
     * 获取题目统计信息
     *
     * @return 统计信息列表，每个难度级别一条记录
     */
    List<Map<String, Object>> getAcceptanceStatistics();
}
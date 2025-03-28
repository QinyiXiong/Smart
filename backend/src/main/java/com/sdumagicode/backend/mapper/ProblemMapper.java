package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.dto.ProblemDTO;
import com.sdumagicode.backend.entity.Problem;
import com.sdumagicode.backend.core.mapper.Mapper;
import java.util.List;

public interface ProblemMapper extends Mapper<Problem> {
    /**
     * 查询题目列表数据
     *
     * @return
     */
    List<ProblemDTO> selectProblems();
}
package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.entity.SpecialDay;
import com.sdumagicode.backend.mapper.SpecialDayMapper;
import com.sdumagicode.backend.service.SpecialDayService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author ronger
 */
@Service
public class SpecialDayServiceImpl extends AbstractService<SpecialDay> implements SpecialDayService {

    @Resource
    private SpecialDayMapper specialDayMapper;

}

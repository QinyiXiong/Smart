package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.entity.CurrencyRule;
import com.sdumagicode.backend.mapper.CurrencyRuleMapper;
import com.sdumagicode.backend.service.CurrencyRuleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author ronger
 */
@Service
public class CurrencyRuleServiceImpl extends AbstractService<CurrencyRule> implements CurrencyRuleService {

    @Resource
    private CurrencyRuleMapper currencyRuleMapper;

}

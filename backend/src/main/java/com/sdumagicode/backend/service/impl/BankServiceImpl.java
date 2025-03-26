package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.dto.BankDTO;
import com.sdumagicode.backend.entity.Bank;
import com.sdumagicode.backend.mapper.BankMapper;
import com.sdumagicode.backend.service.BankService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 银行
 *
 * @author ronger
 */
@Service
public class BankServiceImpl extends AbstractService<Bank> implements BankService {

    @Resource
    private BankMapper bankMapper;

    @Override
    public List<BankDTO> findBanks() {
        List<BankDTO> banks = bankMapper.selectBanks();
        return banks;
    }
}

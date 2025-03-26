package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.dto.BankDTO;
import com.sdumagicode.backend.entity.Bank;

import java.util.List;

/**
 * 银行
 *
 * @author ronger
 */
public interface BankService extends Service<Bank> {
    List<BankDTO> findBanks();
}

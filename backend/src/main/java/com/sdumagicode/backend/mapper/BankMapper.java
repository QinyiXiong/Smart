package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.dto.BankDTO;
import com.sdumagicode.backend.entity.Bank;

import java.util.List;

/**
 * @author ronger
 */
public interface BankMapper extends Mapper<Bank> {
    /**
     * 查询银行列表数据
     *
     * @return
     */
    List<BankDTO> selectBanks();
}

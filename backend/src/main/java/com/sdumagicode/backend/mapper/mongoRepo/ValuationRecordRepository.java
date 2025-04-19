package com.sdumagicode.backend.mapper.mongoRepo;

import com.sdumagicode.backend.entity.chat.ValuationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.mongodb.repository.MongoRepository;

@Mapper
public interface ValuationRecordRepository extends MongoRepository<ValuationRecord,String> {
    ValuationRecord findByChatId(Long chatId);

    boolean deleteByChatId(Long chatId);
}

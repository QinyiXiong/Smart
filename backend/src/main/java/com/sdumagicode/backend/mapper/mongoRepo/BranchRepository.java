package com.sdumagicode.backend.mapper.mongoRepo;

import com.sdumagicode.backend.entity.chat.Branch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BranchRepository extends MongoRepository<Branch, String> {
    
    /**
     * 通过 chatId 查询所有关联的 Branch
     * @param chatId 聊天会话ID
     * @return 该聊天下的所有分支列表
     */
    List<Branch> findByChatId(Long chatId);
    



}

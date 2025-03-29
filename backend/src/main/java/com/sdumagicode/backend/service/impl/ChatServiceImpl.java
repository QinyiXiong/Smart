package com.sdumagicode.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.entity.chat.Branch;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import com.sdumagicode.backend.mapper.ChatMapper;
import com.sdumagicode.backend.mapper.mongoRepo.BranchRepository;
import com.sdumagicode.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl extends AbstractService<ChatRecords> implements ChatService {

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Override
    public List<ChatRecords> getChatRecords(ChatRecords chatRecords) {
        LambdaQueryWrapper<ChatRecords> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ChatRecords::getUserId,chatRecords.getUserId());
        lqw.eq(ChatRecords::getInterviewerId,chatRecords.getInterviewerId());


        return chatMapper.selectByCondition(lqw);
    }

    @Override
    public List<Branch> getAllBranches(ChatRecords chatRecords) {
        return branchRepository.findByChatId(chatRecords.getChatId()+"");
    }
}

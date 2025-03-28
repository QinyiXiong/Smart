package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import com.sdumagicode.backend.mapper.ChatMapper;
import com.sdumagicode.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl extends AbstractService<ChatRecords> implements ChatService {

    @Autowired
    private ChatMapper chatMapper;

    @Override
    public List<ChatRecords> getChatRecords(ChatRecords chatRecords) {
        return null;
    }
}

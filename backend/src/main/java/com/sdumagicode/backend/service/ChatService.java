package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.entity.chat.ChatRecords;

import java.util.List;

public interface ChatService extends Service<ChatRecords> {

    public List<ChatRecords> getChatRecords(ChatRecords chatRecords);
}

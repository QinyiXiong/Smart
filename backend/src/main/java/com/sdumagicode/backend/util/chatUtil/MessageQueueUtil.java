package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.dto.chat.ChatOutput;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageQueueUtil {
    private static final BlockingQueue<ChatOutput> messageQueue = new LinkedBlockingQueue<>();
    
    public static void addMessage(ChatOutput message) {
        messageQueue.offer(message);
    }
    
    public static BlockingQueue<ChatOutput> getQueue() {
        return messageQueue;
    }
}

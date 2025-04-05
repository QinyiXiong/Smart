package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.dto.chat.ChatOutput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//public class MessageQueueUtil {
//    private static final BlockingQueue<ChatOutput> messageQueue = new LinkedBlockingQueue<>();
//
//    public static void addMessage(ChatOutput message) {
//        messageQueue.offer(message);
//    }
//
//    public static BlockingQueue<ChatOutput> getQueue() {
//        return messageQueue;
//    }
//}
public class MessageQueueUtil {

    private static final BlockingQueue<ChatOutput> messageQueue = new LinkedBlockingQueue<>();

    public static void addMessage(ChatOutput message) {
        messageQueue.offer(message);
    }

    public static BlockingQueue<ChatOutput> getQueue() {
        return messageQueue;
    }
    private static final BlockingQueue<ChatOutput> queue =
            new LinkedBlockingQueue<>(1000); // 设置合理容量

    // 使用批处理方式添加消息
    public static void addMessages(Collection<ChatOutput> messages) {
        queue.addAll(messages);
    }

    // 批量获取消息
    public static List<ChatOutput> pollBatch(int maxSize) {
        List<ChatOutput> batch = new ArrayList<>(maxSize);
        queue.drainTo(batch, maxSize);
        return batch;
    }
}
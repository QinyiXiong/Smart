package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.dto.chat.ChatOutput;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


//public class MessageQueueUtil {
//
//    private static final BlockingQueue<ChatOutput> messageQueue = new LinkedBlockingQueue<>();
//
//    public static void addMessage(ChatOutput message) {
//        messageQueue.offer(message);
//    }
//
//    public static BlockingQueue<ChatOutput> getQueue() {
//        return messageQueue;
//    }
//    private static final BlockingQueue<ChatOutput> queue =
//            new LinkedBlockingQueue<>(1000); // 设置合理容量
//
//    // 使用批处理方式添加消息
//    public static void addMessages(Collection<ChatOutput> messages) {
//        queue.addAll(messages);
//    }
//
//    // 批量获取消息
//    public static List<ChatOutput> pollBatch(int maxSize) {
//        List<ChatOutput> batch = new ArrayList<>(maxSize);
//        queue.drainTo(batch, maxSize);
//        return batch;
//    }
//}

public class MessageQueueUtil {
    // 使用ConcurrentHashMap存储不同messageId对应的队列
    private static final ConcurrentHashMap<String, BlockingQueue<ChatOutput>> messageQueues =
            new ConcurrentHashMap<>();

    // 为特定messageId添加消息
    public static void addMessage(ChatOutput message) {
        getOrCreateQueue(message.getMessageId()).offer(message);
    }

    // 批量添加消息
    public static void addMessages(String messageId, Collection<ChatOutput> messages) {
        BlockingQueue<ChatOutput> queue = getOrCreateQueue(messageId);
        queue.addAll(messages);
    }

    // 批量获取消息
    public static List<ChatOutput> pollBatch(String messageId, int maxSize) {
        BlockingQueue<ChatOutput> queue = messageQueues.get(messageId);
        if (queue == null) {
            return Collections.emptyList();
        }

        List<ChatOutput> batch = new ArrayList<>(maxSize);
        queue.drainTo(batch, maxSize);
        return batch;
    }

    // 获取或创建队列
    private static BlockingQueue<ChatOutput> getOrCreateQueue(String messageId) {
        return messageQueues.computeIfAbsent(messageId,
                k -> new LinkedBlockingQueue<>(1000));
    }

    // 清理不再需要的队列
    public static void removeQueue(String messageId) {
        messageQueues.remove(messageId);
    }

    public static void check(){
        // 获取所有活跃的messageId
        Set<String> activeMessageIds = MessageQueueUtil.messageQueues.keySet();
// 获取特定队列的内容
        for(String ids:activeMessageIds){
            System.out.println(ids);
        }
    }
}

package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.dto.chat.ChatOutput;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


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
    // 使用ConcurrentHashMap存储不同messageId对应的队列和最后访问时间
    private static final ConcurrentHashMap<String, QueueWithTimestamp> messageQueues =
            new ConcurrentHashMap<>();

    // 定时任务执行器
    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    // 队列过期时间(毫秒)
    private static final long EXPIRE_TIME_MS = 30_000; // 30秒

    // 静态初始化块，启动定时清理任务
    static {
        // 每10秒检查一次过期队列
        scheduler.scheduleAtFixedRate(
                MessageQueueUtil::cleanExpiredQueues,
                10, 10, TimeUnit.SECONDS);
    }

    // 内部类，包装队列和最后访问时间
    private static class QueueWithTimestamp {
        final BlockingQueue<ChatOutput> queue;
        final AtomicLong lastAccessTime;

        QueueWithTimestamp() {
            this.queue = new LinkedBlockingQueue<>(1000);
            this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        }

        void updateAccessTime() {
            lastAccessTime.set(System.currentTimeMillis());
        }
    }

    // 为特定messageId添加消息
    public static void addMessage(ChatOutput message) {
        QueueWithTimestamp queueWithTime = getOrCreateQueue(message.getMessageId());
        queueWithTime.queue.offer(message);
        queueWithTime.updateAccessTime();
    }

    // 批量添加消息
    public static void addMessages(String messageId, Collection<ChatOutput> messages) {
        QueueWithTimestamp queueWithTime = getOrCreateQueue(messageId);
        queueWithTime.queue.addAll(messages);
        queueWithTime.updateAccessTime();
    }

    // 批量获取消息
    public static List<ChatOutput> pollBatch(String messageId, int maxSize) {
        QueueWithTimestamp queueWithTime = messageQueues.get(messageId);
        System.out.println(queueWithTime);
        if (queueWithTime == null) {
            return Collections.emptyList();
        }

        List<ChatOutput> batch = new ArrayList<>(maxSize);
        queueWithTime.queue.drainTo(batch, maxSize);
        queueWithTime.updateAccessTime();
        return batch;
    }

    // 获取或创建队列
    private static QueueWithTimestamp getOrCreateQueue(String messageId) {
        return messageQueues.computeIfAbsent(messageId, k -> new QueueWithTimestamp());
    }

    // 清理不再需要的队列
    public static void removeQueue(String messageId) {
        messageQueues.remove(messageId);
    }

    // 检查所有活跃队列
    public static void check() {
        Set<String> activeMessageIds = messageQueues.keySet();
        for(String id : activeMessageIds) {
            System.out.println(id);
        }
    }

    // 清理过期队列的方法
    private static void cleanExpiredQueues() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, QueueWithTimestamp>> iterator =
                messageQueues.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, QueueWithTimestamp> entry = iterator.next();
            long lastAccessTime = entry.getValue().lastAccessTime.get();

            if (currentTime - lastAccessTime > EXPIRE_TIME_MS) {
                iterator.remove();
                System.out.println("清理过期队列: " + entry.getKey());
            }
        }
    }

    // 关闭定时任务(在应用关闭时调用)
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
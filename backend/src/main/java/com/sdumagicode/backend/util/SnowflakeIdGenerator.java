package com.sdumagicode.backend.util;

public class SnowflakeIdGenerator {
    private final long workerId;
    private final long epoch = 1609459200000L; // 自定义起始时间（2021-01-01）
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        this.workerId = workerId & 0x3FF; // 10位机器ID
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨！");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 0xFFF; // 12位序列号
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - epoch) << 22) | (workerId << 12) | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}

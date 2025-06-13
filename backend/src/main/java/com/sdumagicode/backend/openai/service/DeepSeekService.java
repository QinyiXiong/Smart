package com.sdumagicode.backend.openai.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API服务类
 * 用于与DeepSeek API进行交互
 */
@Service
public class DeepSeekService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public DeepSeekService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    /**
     * 发送聊天请求到DeepSeek API
     * 
     * @param messages 消息列表
     * @param model 模型名称，默认为 "deepseek-chat"
     * @param temperature 温度参数，控制随机性
     * @return API响应的JSON字符串
     * @throws IOException 如果API请求失败
     */
    public String chatCompletion(List<Map<String, String>> messages, String model, double temperature) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", messages);
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), jsonBody);
            
        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API请求失败: " + response.code() + " " + response.message());
            }
            return response.body().string();
        }
    }
    
    /**
     * 发送聊天请求到DeepSeek API（使用默认参数）
     * 
     * @param messages 消息列表
     * @return API响应的JSON字符串
     * @throws IOException 如果API请求失败
     */
    public String chatCompletion(List<Map<String, String>> messages) throws IOException {
        return chatCompletion(messages, "deepseek-chat", 0.7);
    }
    
    /**
     * 创建聊天消息
     * 
     * @param role 角色 (system, user, assistant)
     * @param content 消息内容
     * @return 包含角色和内容的Map
     */
    public Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
    
    /**
     * 流式聊天请求（SSE）
     * 
     * @param messages 消息列表
     * @param model 模型名称
     * @param temperature 温度参数
     * @param callback 回调函数，用于处理每个SSE事件
     * @throws IOException 如果API请求失败
     */
    public void streamingChatCompletion(List<Map<String, String>> messages, String model, double temperature, Callback callback) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("messages", messages);
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", true);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), jsonBody);
            
        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build();
            
        client.newCall(request).enqueue(callback);
    }
    
    /**
     * 获取可用模型列表
     * 
     * @return 模型列表的JSON字符串
     * @throws IOException 如果API请求失败
     */
    public String listModels() throws IOException {
        Request request = new Request.Builder()
            .url("https://api.deepseek.com/v1/models")
            .addHeader("Authorization", "Bearer " + apiKey)
            .get()
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API请求失败: " + response.code() + " " + response.message());
            }
            return response.body().string();
        }
    }
} 
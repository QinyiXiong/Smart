package com.sdumagicode.backend.util.embeddingUtil;

import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class EmbeddingClient {

    @Value("${siliconflow.api.key}")
    private String apiKey;

    private static final String EMBEDDING_API_URL = "https://api.siliconflow.cn/v1/embeddings";
    private static final String DEFAULT_MODEL = "BAAI/bge-large-zh-v1.5";
    private static final int MAX_TOKEN_COUNT = 500; // 设置稍低于512以确保安全

    /**
     * 获取单个文本的embedding向量
     * @param text 输入文本
     * @return embedding向量列表
     */
    public List<List<Float>> getEmbedding(String text) {
        // 截断文本以确保不超过token限制
        String truncatedText = truncateText(text);
        List<String> texts = new ArrayList<>();
        texts.add(truncatedText);
        return getEmbeddings(texts);
    }


    /**
     * 批量获取文本的embedding向量
     * @param texts 输入文本列表
     * @return embedding向量列表
     */
    public List<List<Float>> getEmbeddings(List<String> texts) {
        try {
            // 截断每个文本以确保不超过token限制
            List<String> truncatedTexts = new ArrayList<>();
            for (String text : texts) {
                truncatedTexts.add(truncateText(text));
            }
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("input", truncatedTexts);
            requestBody.put("encoding_format", "float");

            HttpResponse<String> response = Unirest.post(EMBEDDING_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toJSONString())
                    .asString();

            if (response.isSuccess()) {
                JSONObject responseBody = JSON.parseObject(response.getBody());
                JSONArray data = responseBody.getJSONArray("data");
                List<List<Float>> embeddings = new ArrayList<>();
                
                for (int i = 0; i < data.size(); i++) {
                    JSONObject item = data.getJSONObject(i);
                    JSONArray embeddingArray = item.getJSONArray("embedding");
                    List<Float> embedding = embeddingArray.toJavaList(Float.class);
                    embeddings.add(embedding);
                }
                return embeddings;
            } else {
                throw new RuntimeException("Embedding API request failed: " + response.getStatus() + " - " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get embeddings", e);
        }
    }
    
    /**
     * 截断文本，确保不超过token上限
     * 这是一个简单的启发式方法，实际token数可能因分词算法而异
     * 
     * @param text 需要截断的文本
     * @return 截断后的文本
     */
    private String truncateText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 一个简单的启发式处理：假设平均每个中文字符大约是1个token
        // 英文单词约是1.5个字符1个token
        
        // 统计中文字符数
        int chineseChars = countChineseCharacters(text);
        
        // 统计非中文字符数，如英文单词、数字等
        int nonChineseChars = text.length() - chineseChars;
        
        // 估算token数：中文字符 + 非中文字符/1.5
        int estimatedTokens = chineseChars + (int)(nonChineseChars / 1.5);
        
        if (estimatedTokens <= MAX_TOKEN_COUNT) {
            return text; // 如果估计的token数小于限制，直接返回原文本
        }
        
        // 需要截断
        // 简单方法：根据估算的比例截断
        double ratio = (double)MAX_TOKEN_COUNT / estimatedTokens;
        int newLength = (int)(text.length() * ratio);
        
        // 确保不会截取到一半的UTF-8字符
        if (newLength > 0 && newLength < text.length()) {
            return text.substring(0, newLength) + "...";
        }
        
        return text;
    }
    
    /**
     * 统计中文字符数量
     * @param text 要统计的文本
     * @return 中文字符数量
     */
    private int countChineseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
        int count = 0;
        for (char c : text.toCharArray()) {
            if (pattern.matcher(String.valueOf(c)).matches()) {
                count++;
            }
        }
        
        return count;
    }
}

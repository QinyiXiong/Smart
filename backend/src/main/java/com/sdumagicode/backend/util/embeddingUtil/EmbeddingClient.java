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

@Component
public class EmbeddingClient {

    @Value("${siliconflow.api.key}")
    private String apiKey;

    private static final String EMBEDDING_API_URL = "https://api.siliconflow.cn/v1/embeddings";
    private static final String DEFAULT_MODEL = "BAAI/bge-large-zh-v1.5";

    /**
     * 获取单个文本的embedding向量
     * @param text 输入文本
     * @return embedding向量列表
     */
    public List<List<Float>> getEmbedding(String text) {
        List<String> texts = new ArrayList<>();
        texts.add(text);
        return getEmbeddings(texts);
    }


    /**
     * 批量获取文本的embedding向量
     * @param texts 输入文本列表
     * @return embedding向量列表
     */
    public List<List<Float>> getEmbeddings(List<String> texts) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("input", texts);
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
            throw new RuntimeException("Failed to get embeddings", e);
        }
    }
}

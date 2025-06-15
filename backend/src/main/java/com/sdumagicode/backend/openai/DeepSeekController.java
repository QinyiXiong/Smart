package com.sdumagicode.backend.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.openai.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek API控制器
 * 提供DeepSeek模型API的接口
 */
@RestController
@RequestMapping("/api/v1/deepseek")
public class DeepSeekController {

    @Autowired
    private DeepSeekService deepSeekService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取可用的模型列表
     *
     * @return 模型列表
     */
    @GetMapping("/models")
    public GlobalResult<String> getModels() {
        try {
            String models = deepSeekService.listModels();
            return GlobalResultGenerator.genSuccessResult(models);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("获取模型列表失败：" + e.getMessage());
        }
    }

    /**
     * 发送聊天请求
     *
     * @param requestBody 请求体，包含messages, model, temperature
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public GlobalResult<String> chatCompletion(@RequestBody Map<String, Object> requestBody) {
        try {
            // 抑制编译器对未经检查的类型转换所发出的警告。
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = (List<Map<String, String>>) requestBody.get("messages");
            String model = requestBody.containsKey("model") ? (String) requestBody.get("model") : "deepseek-chat";
            double temperature = requestBody.containsKey("temperature") ? 
                Double.parseDouble(requestBody.get("temperature").toString()) : 0.7;
                
            String response = deepSeekService.chatCompletion(messages, model, temperature);
            return GlobalResultGenerator.genSuccessResult(response);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("聊天请求失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成对话摘要
     * 
     * @param requestBody 包含对话消息和chatId的请求体
     * @return 生成的摘要
     */
    @PostMapping("/summarize")
    public GlobalResult<String> summarizeConversation(@RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
            
            // 准备系统提示词和用户消息
            List<Map<String, String>> promptMessages = new ArrayList<>();
            
            // 添加系统提示
            Map<String, String> systemPrompt = new HashMap<>();
            systemPrompt.put("role", "system");
            systemPrompt.put("content", "你是一个专业的面试对话摘要生成器。请根据以下面试对话生成一个简短的标题，标题应概括对话的主要内容。"
                    + "标题必须精炼，不超过20个字，不要加任何前缀和标点符号，直接输出标题文本。标题应突出面试的主要话题或技能领域。");
            promptMessages.add(systemPrompt);
            
            // 构建对话历史
            StringBuilder conversationBuilder = new StringBuilder();
            for (Map<String, Object> message : messages) {
                String role = (String) message.get("role");
                String content = (String) message.get("content");
                
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                
                conversationBuilder.append(role.equals("assistant") ? "面试官: " : "候选人: ");
                conversationBuilder.append(content).append("\n\n");
            }
            
            // 添加用户消息，包含对话内容
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "以下是一段面试对话，请为其生成一个简短的标题：\n\n" + conversationBuilder.toString());
            promptMessages.add(userMessage);
            
            // 调用DeepSeek API生成摘要，使用较低的温度以获得更确定性的结果
            String response = deepSeekService.chatCompletion(promptMessages, "deepseek-chat", 0.3);
            
            // 解析响应，提取摘要文本
            JsonNode responseJson = objectMapper.readTree(response);
            String summary = responseJson.path("choices").get(0).path("message").path("content").asText();
            
            // 清理摘要文本，去除多余的引号、空格和换行符
            summary = summary.replaceAll("\"", "").trim();
            summary = summary.replaceAll("\\r?\\n", " ").trim();
            
            return GlobalResultGenerator.genSuccessResult(summary);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("生成摘要失败：" + e.getMessage());
        }
    }

    /**
     * 简单聊天测试接口
     *
     * @param message 用户消息内容
     * @return 聊天响应
     */

//    {
//        "code": 0,
//            "message": "{\"id\":\"1b69bd36-0682-494c-aea6-4b7d93652542\",\"object\":\"chat.completion\",\"created\":1749568550,\"model\":\"deepseek-chat\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"你好！😊 很高兴见到你！有什么我可以帮你的吗？\"},\"logprobs\":null,\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":15,\"total_tokens\":25,\"prompt_tokens_details\":{\"cached_tokens\":0},\"prompt_cache_hit_tokens\":0,\"prompt_cache_miss_tokens\":10},\"system_fingerprint\":\"fp_8802369eaa_prod0425fp8\"}",
//            "success": true
//    }
    @GetMapping("/test")
    public GlobalResult<String> testChat(@RequestParam String message) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(deepSeekService.createMessage("system", "你是一个有用的AI助手。"));
            messages.add(deepSeekService.createMessage("user", message));
            
            String response = deepSeekService.chatCompletion(messages);
            return GlobalResultGenerator.genSuccessResult(response);
        } catch (Exception e) {
            return GlobalResultGenerator.genErrorResult("测试请求失败：" + e.getMessage());
        }
    }
} 
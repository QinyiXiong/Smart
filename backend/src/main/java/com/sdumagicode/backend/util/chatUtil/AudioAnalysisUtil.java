package com.sdumagicode.backend.util.chatUtil;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class AudioAnalysisUtil {

    @Value("${openai.api.key}")
    private String apiKey;

    /**
     * 分析面试语音内容及情感状态
     * @param audioFilePath 音频文件路径
     * @return 格式化分析结果
     */
    public String analyzeInterviewAudio(String audioFilePath) throws ApiException, NoApiKeyException, UploadFileException {
        // 构建系统提示词 - 以面试官角度分析
        String systemPrompt = "你是一名专业的面试官，请分析以下面试者的语音回答。"
                + "你需要完成以下任务："
                + "1. 准确转录语音内容，用<语音内容></语音内容>标签包裹"
                + "2. 分析面试者的情感状态（是否紧张、自信等），用<情感分析></情感分析>标签包裹"
                + "请确保输出格式严格遵循上述要求";

        MultiModalConversation conv = new MultiModalConversation();
        
        // 构建用户消息
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        createAudioContent(audioFilePath),
                        createTextContent("请分析这段面试回答")
                ))
                .build();
        
        // 构建系统消息
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Arrays.asList(createTextContent(systemPrompt)))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-audio-turbo-latest")
                .apiKey(apiKey)  // 注入API Key
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        MultiModalConversationResult result = conv.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent().toString();
    }

    private Map<String, Object> createAudioContent(String filePath) {
        return new HashMap<String, Object>() {{
            put("audio", filePath);
        }};
    }

    private Map<String, Object> createTextContent(String text) {
        return new HashMap<String, Object>() {{
            put("text", text);
        }};
    }
}

package com.sdumagicode.backend.util.chatUtil;


import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.sdumagicode.backend.entity.chat.MessageLocal;
import io.reactivex.Flowable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatUtil {
    // 应用ID配置
    @Value("${interviewer.app.id}")
    private String APP_ID_INTERVIEWER;

    @Value("${coder.app.id}")
    private String APP_ID_CODER;

    @Value("${openai.api.key}")
    private String apiKey;

    private String appId;


    
    /**
     * 流式调用AI接口
     * @param messageList 消息列表
     * @return Flowable<ApplicationResult>
     * @throws ApiException API异常
     * @throws NoApiKeyException 无API Key异常
     * @throws InputRequiredException 输入必需异常
     */
    public Flowable<ApplicationResult> streamCall(List<MessageLocal> messageList,String prompt,AppType appType)
            throws ApiException, NoApiKeyException, InputRequiredException {
        // 转换消息格式
        List<Message> messages = convertMessages(messageList,prompt);
//        System.out.println("apikey: " + apiKey);
//        System.out.println("app_id: " + APP_ID_INTERVIEWER);

        if (appType == AppType.INTERVIEWER){
            appId = APP_ID_INTERVIEWER;
        }else if(appType == AppType.CODER){
            appId = APP_ID_CODER;
        }
        // 构建参数
        ApplicationParam param = ApplicationParam.builder()
                .apiKey(apiKey)
                .appId(appId)
                .incrementalOutput(true)
                .hasThoughts(true)
                .messages(messages)
                .build();
        
        // 调用接口
        Application application = new Application();
        return application.streamCall(param);
    }
    
    /**
     * 同步调用AI接口
     * @param messageList 消息列表
     * @return ApplicationResult
     * @throws ApiException API异常
     * @throws NoApiKeyException 无API Key异常
     * @throws InputRequiredException 输入必需异常
     */
//    public ApplicationResult call(List<MessageLocal> messageList)
//            throws ApiException, NoApiKeyException, InputRequiredException {
//        // 转换消息格式
//        List<Message> messages = convertMessages(messageList);
//
//        // 构建参数
//        ApplicationParam param = ApplicationParam.builder()
//                .apiKey(this.apiKey)
//                .appId(this.appId)
//                .incrementalOutput(false)
//                .hasThoughts(true)
//                .messages(messages)
//                .build();
//
//        // 调用接口
//        Application application = new Application();
//        return application.call(param);
//    }
    
    /**
     * 转换消息格式
     * @param messageList 原始消息列表
     * @return 转换后的消息列表
     */
    private List<Message> convertMessages(List<MessageLocal> messageList,String prompt) {
        List<Message> messages = new ArrayList<>();
        
        // 添加系统提示
        if (prompt != null && !prompt.isEmpty()) {
            messages.add(Message.builder()
                    .role("system")
                    .content(prompt)
                    .build());
        }
        
        // 添加用户和助手消息
        messages.addAll(messageList.stream()
                .map(msg -> Message.builder()
                        .role(msg.getRole())
                        .content(msg.getContent().getText())
                        .build())
                .collect(Collectors.toList()));
        
        return messages;
    }
    
    /**
     * 应用类型枚举
     */
    public enum AppType {
        INTERVIEWER, CODER
    }
}

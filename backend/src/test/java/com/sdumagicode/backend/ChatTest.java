package com.sdumagicode.backend;// 建议dashscope SDK的版本 >= 2.18.2
import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.List;


public class ChatTest {
    public static void appCall()
            throws ApiException, NoApiKeyException, InputRequiredException {
        List<Message> messages = new ArrayList<>();

        messages.add(Message.builder().role("system").content("You are a helpful assistant.").build());
        messages.add(Message.builder().role("user").content("你是谁？").build());
        messages.add(Message.builder().role("assistant").content("我是阿里云开发的大规模语言模型，我叫通义千问。").build());
        messages.add(Message.builder().role("user").content("你能做什么？").build());

        ApplicationParam param = ApplicationParam.builder()
                // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
                .apiKey("sk-d27f5c8bb2c6423f8387ea1c3b845bb3")
                .appId("dcb40adf51d04a3d82b75d8f316a6140")
                .incrementalOutput(false)
                .hasThoughts(true)
                .messages(messages)
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);
        //ApplicationResult result = application.call(param);
        result.blockingForEach(data -> {
            System.out.println("思考过程：");
            System.out.println(data.getOutput().getThoughts());
            System.out.println("回复内容：");
            System.out.printf(data.getOutput().getText());
        });

    }

    public static void main(String[] args) {
        try {
            appCall();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.printf("Exception: %s", e.getMessage());
            System.out.println("请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code");
        }
        System.exit(0);
    }
}
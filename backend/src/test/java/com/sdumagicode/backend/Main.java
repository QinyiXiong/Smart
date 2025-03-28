package com.sdumagicode.backend;

import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;


public class Main{
      public static void callAgentApp()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
            // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
            .apiKey("sk-d27f5c8bb2c6423f8387ea1c3b845bb3")
            .appId("dcb40adf51d04a3d82b75d8f316a6140")
            .prompt("请帮我推荐一款3000元以下的百炼手机")
            .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);
          System.out.println("思考过程：");
          System.out.println(result.getOutput().getThoughts());
          System.out.println("回复内容：");
          System.out.printf(result.getOutput().getText());
    }


    public static void main(String[] args) {
        try {
            callAgentApp();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {

            System.out.printf("Exception: %s", e.getMessage());
        }
        System.exit(0);
    }  
}
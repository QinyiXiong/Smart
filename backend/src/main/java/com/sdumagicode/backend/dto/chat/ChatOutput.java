package com.sdumagicode.backend.dto.chat;

import com.alibaba.dashscope.app.ApplicationOutput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatOutput {

    public ChatOutput(ApplicationOutput output){
        this.finish = output.getFinishReason();
        this.text = output.getText();
        if(output.getThoughts() != null){
            this.thoughts = output.getThoughts().toString();
        }
    }

    private String text;
    private String thoughts;
    private String finish;

    private String messageId;
    private Long userId;

    public ChatOutput(String s) {
        this.text = s;
    }

}

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
        this.thoughts = output.getThoughts().toString();
    }

    private String text;
    private String thoughts;
    private String finish;

    public ChatOutput(String s) {
        this.text = s;
    }
}

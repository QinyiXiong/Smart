package com.sdumagicode.backend.entity.chat;

import lombok.Data;
import org.springframework.data.annotation.Id;
import java.util.Date;

@Data
public class Message {

    @Id
    private String id;

    private String conversationId;
    private String branchId;
    private String messageId;
    private String role; // "user" or "assistant"
    private String inputType; // "text", "voice", "mixed"
    private Content content;
    private Date timestamp;
}

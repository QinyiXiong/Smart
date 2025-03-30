package com.sdumagicode.backend.entity.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageLocal {

    @Id
    private String id;


    private String branchId;
    private String messageId;
    private String role; // "user" or "assistant"
    private String inputType; // "text", "voice", "mixed"
    private Content content;
    private Date timestamp;
}

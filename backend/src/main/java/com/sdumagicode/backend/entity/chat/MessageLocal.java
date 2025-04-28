package com.sdumagicode.backend.entity.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageLocal {

    @Id
    private String messageId;


    private String branchId;

    private String role; // "user" or "assistant"
    private String inputType; // "text", "voice", "mixed"
    private Content content;
    private Date timestamp;
}

package com.sdumagicode.backend.dto.chat;

import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.entity.chat.MessageLocal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private List<MessageLocal> messageList;
    private Interviewer interviewer;
}

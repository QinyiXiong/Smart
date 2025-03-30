package com.sdumagicode.backend.service;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.entity.chat.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

public interface ChatService extends Service<ChatRecords> {

    public List<ChatRecords> getChatRecords(ChatRecords chatRecords);

    public  List<Branch> getAllBranches(ChatRecords chatRecords);

    public Flux<GlobalResult<ApplicationOutput>> sendMessageAndGetFlux(List<MessageLocal> messageList, String Prompt);

    public Flux<GlobalResult<ApplicationOutput>> sendMessageToInterviewerAndGetFlux(List<MessageLocal> messageList,Interviewer interviewer);

    public MessageFileDto convertMessageFile(MultipartFile multipartFile) throws IOException;
}

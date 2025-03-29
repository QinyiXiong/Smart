package com.sdumagicode.backend.controller.chat;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.entity.chat.Branch;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.entity.chat.MessageLocal;
import com.sdumagicode.backend.service.ChatService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiresPermissions(value = "user")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/getChatRecords")
    public GlobalResult<List<ChatRecords>> getChatRecords(@RequestBody ChatRecords chatRecords){
        if(chatRecords.getUserId() == null || chatRecords.getInterviewerId() == null){
            throw  new ServiceException("缺少关键信息");
        }


        return GlobalResultGenerator.genSuccessResult(chatService.getChatRecords(chatRecords));
    }

    @PostMapping("/getAllBranches")
    public GlobalResult<List<Branch>> getAllBranches(@RequestBody ChatRecords chatRecords){
        if(chatRecords.getChatId() == null){
            throw  new ServiceException("缺少关键信息");
        }
        return GlobalResultGenerator.genSuccessResult(chatService.getAllBranches(chatRecords));
    }

    @PostMapping("/sendMessage")
    public GlobalResult<MessageLocal> sendMessage(@RequestBody List<MessageLocal> messageList, @RequestBody Interviewer interviewer){
        return null;

    }
}

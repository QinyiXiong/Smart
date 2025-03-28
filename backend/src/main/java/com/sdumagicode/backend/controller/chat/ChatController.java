package com.sdumagicode.backend.controller.chat;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiresPermissions(value = "user")
public class ChatController {

    @PostMapping("/getChatRecords")
    public GlobalResult<List<ChatRecords>> getChatRecords(@RequestBody ChatRecords chatRecords){
        if(chatRecords.getUserId() == null || chatRecords.getInterviewerId() == null){
            throw  new ServiceException("缺少关键信息");
        }


        return null;
    }
}

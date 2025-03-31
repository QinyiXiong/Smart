package com.sdumagicode.backend.controller.chat;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.entity.chat.*;
import com.sdumagicode.backend.service.ChatService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiresPermissions(value = "user")
public class ChatController {

    @Autowired
    private ChatService chatService;


    @PostMapping("/getChatRecords")
    public GlobalResult<List<ChatRecords>> getChatRecords(@RequestBody ChatRecords chatRecords){
        if(chatRecords.getInterviewerId() == null){
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
    public Flux<GlobalResult<ApplicationOutput>> sendMessage(@RequestBody List<MessageLocal> messageList, @RequestBody Interviewer interviewer){
        if(messageList == null||messageList.isEmpty()){
            throw  new ServiceException("缺少发送信息");
        }

        if(interviewer == null){
            throw new ServiceException("未设置面试官");
        }


        return chatService.sendMessageToInterviewerAndGetFlux(messageList,interviewer);

    }

    @PostMapping("/saveBranches")
    public GlobalResult saveBranches(@RequestBody List<Branch> branchList){
        boolean res = chatService.saveBranches(branchList);
        if(res){
            return GlobalResultGenerator.genSuccessResult();
        }else{
            return GlobalResultGenerator.genErrorResult("保存失败");
        }
    }

    @PostMapping("/uploadMessageFile")
    public GlobalResult<MessageFileDto> convertMessage(@RequestParam("file") MultipartFile file) throws IOException {
        return GlobalResultGenerator.genSuccessResult(chatService.convertMessageFile(file));
    }
}

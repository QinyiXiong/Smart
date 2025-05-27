package com.sdumagicode.backend.controller.chat;

import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.service.ChatService;
import com.sdumagicode.backend.service.InterviewerService;
import com.sdumagicode.backend.util.UserUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/share")
@RequiresPermissions(value = "user")
public class ShareController {
    @Autowired
    ChatService chatService;
    @Autowired
    InterviewerService interviewerService;


    @PostMapping("/chatShareToUser")
    public GlobalResult chatShareToUser(@RequestBody Long chatId){
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        chatService.deepCopy(chatId,idUser);
        return GlobalResultGenerator.genSuccessResult("获取分享聊天记录成功");
    }

    @PostMapping("/chatShareToSnapshot")
    public GlobalResult chatShareToSnapshot(@RequestBody Long chatId){
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        ChatRecords chatRecords = chatService.deepCopy(chatId, 2L);
        return GlobalResultGenerator.genSuccessResult(chatRecords);
    }

    @PostMapping("/interviewShareToUser")
    public GlobalResult interviewShareToUser(@RequestBody String interviewerId){
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();

        interviewerService.deepCopy(interviewerId,idUser);
        return GlobalResultGenerator.genSuccessResult("获取分享面试官成功");
    }

    @PostMapping("/interviewShareToSnapshot")
    public GlobalResult interviewShareToSnapshot(@RequestBody String interviewerId){
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();

        Interviewer interviewer = interviewerService.deepCopy(interviewerId, idUser);
        return GlobalResultGenerator.genSuccessStringDataResult(interviewer);
    }



}

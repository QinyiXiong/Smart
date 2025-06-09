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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/share")
@RequiresPermissions(value = "user")
public class ShareController {
    @Autowired
    ChatService chatService;
    @Autowired
    InterviewerService interviewerService;


    // 聊天记录保存到收藏的用户账户中
    @PostMapping("/chatShareToUser")
    public GlobalResult chatShareToUser(@RequestBody InterviewRequest request){
        System.out.println(request.getChatId());
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        chatService.deepCopy(request.getChatId(),idUser);
        return GlobalResultGenerator.genSuccessResult("获取分享聊天记录成功");
    }

    @PostMapping("/chatShareToSnapshot")
    public GlobalResult chatShareToSnapshot(@RequestBody InterviewRequest request){
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        ChatRecords chatRecords = chatService.deepCopy(request.getChatId(), 2L);
        return GlobalResultGenerator.genSuccessResult(chatRecords);
    }

    // 面试官保存到收藏的用户账户中
    @PostMapping("/interviewShareToUser")
    public GlobalResult interviewShareToUser(@RequestBody InterviewerRequest request){

        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();

        interviewerService.deepCopy(request.getInterviewerId(), idUser);
        return GlobalResultGenerator.genSuccessResult("获取分享面试官成功");
    }

    @PostMapping("/interviewShareToSnapshot")
    public GlobalResult interviewShareToSnapshot(@RequestBody InterviewerRequest request){
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();

        Interviewer interviewer = interviewerService.deepCopy(request.getInterviewerId(), idUser);
        return GlobalResultGenerator.genSuccessStringDataResult(interviewer);
    }
    
    @GetMapping("/getUserInterviewers")
    public GlobalResult getUserInterviewers() {
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        List<Interviewer> interviewers = interviewerService.findInterviewers().stream()
                .filter(interviewer -> interviewer.getUserId() != null && interviewer.getUserId().equals(idUser))
                .collect(Collectors.toList());
        return GlobalResultGenerator.genSuccessResult(interviewers);
    }

    @GetMapping("/getUserChatRecords")
    public GlobalResult getUserChatRecords() {
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        
        // 1. 查询用户的所有面试官
        List<Interviewer> interviewers = interviewerService.findInterviewers();
        
        // 2. 收集所有面试记录
        List<ChatRecords> allRecords = new ArrayList<>();
        for (Interviewer interviewer : interviewers) {
            ChatRecords query = new ChatRecords();
            query.setUserId(idUser);
            query.setInterviewerId(interviewer.getInterviewerId());
            allRecords.addAll(chatService.getChatRecords(query));
        }
        
        return GlobalResultGenerator.genSuccessResult(allRecords);
    }

    static class InterviewerRequest {
        private String interviewerId;
        
        public String getInterviewerId() {
            return interviewerId;
        }
        
        public void setInterviewerId(String interviewerId) {
            this.interviewerId = interviewerId;
        }
    }

    static class InterviewRequest {
        private Long chatId;

        public Long getChatId() {
            return chatId;
        }

        public void setChatId(Long chatId) {
            this.chatId = chatId;
        }
    }

}

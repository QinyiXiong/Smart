package com.sdumagicode.backend.controller.chat;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationResult;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.chat.ChatOutput;
import com.sdumagicode.backend.dto.chat.ChatRequest;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.entity.chat.*;
import com.sdumagicode.backend.service.ChatService;
import com.sdumagicode.backend.util.UserUtils;
import com.sdumagicode.backend.util.chatUtil.MessageQueueUtil;
import io.reactivex.Flowable;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/chat")
@RequiresPermissions(value = "user")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/saveChatRecords")
    public GlobalResult<ChatRecords> saveChatRecords(@RequestBody ChatRecords chatRecords){

        ChatRecords _chatRecord = chatService.saveChatRecords(chatRecords);
        return GlobalResultGenerator.genSuccessResult(_chatRecord);
    }

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


    @PostMapping(value = "/sendMessageWithFlux" ,produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GlobalResult<ChatOutput>> sendMessageWithFlux(@RequestBody ChatRequest chatRequest){
        if (chatRequest.getMessageList() == null || chatRequest.getMessageList().isEmpty()) {
            throw new ServiceException("缺少发送信息");
        }
        if (chatRequest.getInterviewer() == null) {
            throw new ServiceException("未设置面试官");
        }
        Flux<ChatOutput> globalResultFlux = chatService.sendMessageToInterviewerAndGetFlux(chatRequest.getMessageList(), chatRequest.getInterviewer());

        return globalResultFlux.map(GlobalResultGenerator::genSuccessResult);

    }



    @PostMapping("/sendMessageWithPoll")
    public GlobalResult sendMessage(@RequestBody ChatRequest chatRequest) {
        // 参数验证
        if (chatRequest.getMessageList() == null || chatRequest.getMessageList().isEmpty()) {
            throw new ServiceException("缺少发送信息");
        }
        if (chatRequest.getInterviewer() == null) {
            throw new ServiceException("未设置面试官");
        }
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        // 异步处理消息
        try {
            chatService.sendMessageToInterviewer(
                    chatRequest.getMessageList(),
                    chatRequest.getInterviewer(),
                    idUser,
                    output -> {
                        // 将每个输出添加到队列

                        MessageQueueUtil.addMessage(output);
                        System.out.println("add queue: "+ output.getText());
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            MessageQueueUtil.addMessage(new ChatOutput("系统错误: " + e.getMessage()));
        }
        return GlobalResultGenerator.genSuccessResult("发送成功");
    }
//    @GetMapping("/pollMessage")
//    public GlobalResult<ChatOutput> pollMessage() throws InterruptedException {
//        // 设置超时时间（例如30秒）
//        ChatOutput message = MessageQueueUtil.getQueue().poll(30, TimeUnit.SECONDS);
//        if (message == null) {
//            return GlobalResultGenerator.genErrorResult("请求超时");
//        }
//        return GlobalResultGenerator.genSuccessResult(message);
//    }

    @GetMapping("/pollMessages")
    public GlobalResult<List<ChatOutput>> pollMessages() {
        List<ChatOutput> batch = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 5秒超时

        // 轮询等待新消息
        while (batch.isEmpty() && (System.currentTimeMillis() - startTime) < timeout) {
            ChatOutput message;
            while((message = MessageQueueUtil.getQueue().poll()) != null && batch.size() < 5) {
                System.out.println("get queue:"+message.getText());
                batch.add(message);
            }

            // 如果还没有消息，短暂休眠避免CPU空转
            if (batch.isEmpty()) {
                try {
                    Thread.sleep(200); // 200毫秒检查一次
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return GlobalResultGenerator.genErrorResult("轮询被中断");
                }
            }
        }

        return GlobalResultGenerator.genSuccessResult(batch);
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

    /**
     * 删除聊天记录
     * @param chatRecords 包含要删除的聊天记录信息
     * @return 操作结果
     */
    @PostMapping("/deleteChatRecords")
    public GlobalResult deleteChatRecords(@RequestBody ChatRecords chatRecords) {
        if(chatRecords.getChatId() == null) {
            throw new ServiceException("缺少聊天记录ID");
        }
        boolean result = chatService.deleteChatRecords(chatRecords);
        if(result) {
            return GlobalResultGenerator.genSuccessResult("删除成功");
        } else {
            return GlobalResultGenerator.genErrorResult("删除失败");
        }
    }
}

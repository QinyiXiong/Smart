package com.sdumagicode.backend.controller.chat;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.dto.chat.ChatOutput;
import com.sdumagicode.backend.dto.chat.ChatRequest;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.dto.chat.MessageLocalDto;
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

import javax.validation.Valid;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        List<Branch> allBranches = chatService.getAllBranches(chatRecords);
        return GlobalResultGenerator.genSuccessResult(allBranches);
    }


//    @PostMapping(value = "/sendMessageWithFlux" ,produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<GlobalResult<ChatOutput>> sendMessageWithFlux(@RequestBody ChatRequest chatRequest){
//        if (chatRequest.getMessageList() == null || chatRequest.getMessageList().isEmpty()) {
//            throw new ServiceException("缺少发送信息");
//        }
//        if (chatRequest.getInterviewer() == null) {
//            throw new ServiceException("未设置面试官");
//        }
//        Flux<ChatOutput> globalResultFlux = chatService.sendMessageToInterviewerAndGetFlux(chatRequest.getMessageList(), chatRequest.getInterviewer());
//
//        return globalResultFlux.map(GlobalResultGenerator::genSuccessResult);
//
//    }



    @PostMapping("/sendMessageWithPoll")
    public GlobalResult<String> sendMessage(
            @RequestParam(value = "chatRequest") String chatRequestStr,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "fileMessageId", required = false) String fileMessageId
    ) throws JsonProcessingException {
        // 手动解析JSON字符串
        ObjectMapper objectMapper = new ObjectMapper();
        ChatRequest chatRequest = objectMapper.readValue(chatRequestStr, ChatRequest.class);
        List<MessageLocal> messageList = chatRequest.getMessageList();
        List<MessageLocalDto> collect = messageList.stream().map((item) -> {
            MessageLocalDto messageLocalDto = new MessageLocalDto(item);
            if(fileMessageId != null && !fileMessageId.isEmpty()){
                if (Objects.equals(messageLocalDto.getMessageId(), fileMessageId)) {
                    messageLocalDto.setUploadFiles(files);
                }
            }


            return messageLocalDto;
        }).collect(Collectors.toList());
        List<MessageLocal> messageLocals = chatService.convertMessageListDto(collect);
        Interviewer interviewer = chatRequest.getInterviewer();
        // 参数验证
        if (messageList == null || messageList.isEmpty() || messageList.get(messageList.size() - 1).getContent().getText().isEmpty()) {
            throw new ServiceException("缺少发送信息");
        }
        if (interviewer == null) {
            throw new ServiceException("未设置面试官");
        }
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        // 异步处理消息

        String messageId = String.valueOf(UUID.randomUUID());
        try {
                    chatService.sendMessageToInterviewer(
                            messageLocals,
                    interviewer,
                    idUser,
                    messageId,
                    output -> {
                        // 将每个输出添加到队列

                        MessageQueueUtil.addMessage(output);
                        //System.out.println("add queue: "+ output.getText());
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            MessageQueueUtil.addMessage(new ChatOutput("系统错误: " + e.getMessage()));
        }
        return GlobalResultGenerator.genSuccessStringDataResult(messageId);
    }


    @GetMapping("/pollMessages")
    public GlobalResult<List<ChatOutput>> pollMessages(
            @RequestParam("messageId") String messageId,
            @RequestParam(defaultValue = "10") int batchSize) {
//        MessageQueueUtil.check();
//        System.out.println("messageId: "+ messageId);
        // 参数校验
        if (messageId == null || messageId.isEmpty()) {
            return GlobalResultGenerator.genErrorResult("messageId不能为空");
        }
        if (batchSize <= 0 || batchSize > 100) {
            batchSize = 10; // 设置合理的默认值
        }
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        long startTime = System.currentTimeMillis();
        final long timeout = 5000; // 5秒超时
        final long pollInterval = 200; // 轮询间隔200ms
        List<ChatOutput> batch = new ArrayList<>(batchSize);
        boolean hasStopSignal = false; // 新增状态位

        try {
            while ((System.currentTimeMillis() - startTime) < timeout) {


                List<ChatOutput> messages = MessageQueueUtil.pollBatch(messageId, batchSize);



                if (!messages.isEmpty()) {
                    //增加验证信息
                    ChatOutput chatOutput = messages.get(0);
                    if(!userId.equals(chatOutput.getUserId())){
                        throw new ServiceException("验证失败");
                    }


                    // 检查当前批次是否有stop信号
                    hasStopSignal = messages.stream()
                            .anyMatch(msg -> "stop".equals(msg.getFinish())) ;

                    batch.addAll(messages);


                    if (hasStopSignal) {
                        MessageQueueUtil.removeQueue(messageId);
                        break;
                    }

                    // 如果有消息立即返回，不等待超时
                    if (!batch.isEmpty()) {
                        break;
                    }
                }

                Thread.sleep(pollInterval);
            }

            return GlobalResultGenerator.genSuccessResult(batch);
        } catch (Exception e) {
            e.printStackTrace();
            return GlobalResultGenerator.genErrorResult("获取消息失败");
        }
    }





    @PostMapping("/saveBranches")
    public GlobalResult saveBranches(@RequestBody List<Branch> branchList){
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        if(branchList == null || branchList.isEmpty()){
            throw new ServiceException("缺少保存列表");
        }
        for (Branch branch:branchList) {
            branch.setUserId(userId);
        }
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

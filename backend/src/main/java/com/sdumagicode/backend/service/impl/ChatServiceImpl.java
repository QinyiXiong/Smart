package com.sdumagicode.backend.service.impl;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.dto.chat.ChatOutput;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.entity.chat.*;
import com.sdumagicode.backend.mapper.ChatMapper;
import com.sdumagicode.backend.mapper.mongoRepo.BranchRepository;
import com.sdumagicode.backend.service.ChatService;
import com.sdumagicode.backend.util.UserUtils;
import com.sdumagicode.backend.util.chatUtil.ChatUtil;
import com.sdumagicode.backend.util.chatUtil.FileUploadUtil;
import com.sdumagicode.backend.util.chatUtil.InterviewerPromptGenerator;
import com.sdumagicode.backend.util.embeddingUtil.MilvusClient;
import io.reactivex.Flowable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class ChatServiceImpl  implements ChatService {

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    FileAnalysisService fileAnalysisService;

    @Autowired
    MilvusClient milvusClient;

    @Autowired
    ChatUtil chatUtil;

    @Override
    public List<ChatRecords> getChatRecords(ChatRecords chatRecords) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        List<ChatRecords> chatRecordsList = chatMapper.selectChatRecords(userId, chatRecords);
        return chatRecordsList;
    }

    @Override
    public ChatRecords saveChatRecords(ChatRecords chatRecords) {
        chatRecords.setUserId(UserUtils.getCurrentUserByToken().getIdUser());
        if(chatRecords.getChatId() == null){
            chatRecords.setCreatedAt(LocalDateTime.now());
            chatRecords.setUpdatedAt(LocalDateTime.now());
            // 使用 insert 方法，会自动填充主键
            chatMapper.insertChatRecord(chatRecords);
        }else{
            chatRecords.setUpdatedAt(LocalDateTime.now());
            chatMapper.updateChatRecord(chatRecords);
        }
        return chatRecords;
    }

    @Override
    public boolean deleteChatRecords(ChatRecords chatRecords) {

        //先删除对应的branch
        branchRepository.deleteAll(branchRepository.findByChatId(chatRecords.getChatId()));

        chatMapper.deleteChatRecord(chatRecords.getChatId());

        return true;
    }

    @Override
    public List<Branch> getAllBranches(ChatRecords chatRecords) {
        List<Branch> byChatId = branchRepository.findByChatId(chatRecords.getChatId());
        return byChatId;
    }

    @Override
    public Flux<ChatOutput> sendMessageAndGetFlux(List<MessageLocal> messageList, String Prompt, ChatUtil.AppType appType) {
        try {
            // 1. 调用AI接口并转换为Flux流
            Flowable<ApplicationResult> aiStream = chatUtil.streamCall(messageList, Prompt, appType);
            aiStream.subscribe(
                    data -> System.out.println("Received: " + data),
                    error -> System.out.println("Error in streamCall: " + error) // 检查是否这里先报错
            );
            // 2. 将Flowable转换为Flux并映射为ChatRecords
            return Flux.from(aiStream)
                    .map(applicationResult -> {
                        // 创建ChatOutput对象
                        ChatOutput chatOutput = new ChatOutput(applicationResult.getOutput());

                        return chatOutput;
                    })
                    .onErrorResume(error -> {
                        System.out.println(error.getMessage());
                        System.out.println(error.getCause());
                        return Flux.error(new ServiceException("转换流错误: " + error.getMessage()));
                    });

        } catch (Exception e) {

            throw new ServiceException("处理异常: " + e.getMessage());
        }
    }

    @Override
    public Flux<ChatOutput> sendMessageToInterviewerAndGetFlux(List<MessageLocal> messageList, Interviewer interviewer) {
        //对用户最后一条信息进行RAG搜索
        MessageLocal messageLocal = messageList.get(messageList.size() - 1);

        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        milvusClient.buildRAGContent(idUser,interviewer.getKnowledgeBaseId(),messageLocal.getContent().getText(),5);


        return sendMessageAndGetFlux(messageList,InterviewerPromptGenerator.generatePrompt(interviewer), ChatUtil.AppType.INTERVIEWER);
    }

    @Override
    @Async
    public void sendMessageToInterviewer(List<MessageLocal> messageList, Interviewer interviewer,Long userId,String messageId, Consumer<ChatOutput> outputConsumer) {
        try {
            // 1. RAG搜索
            MessageLocal lastMessage = messageList.get(messageList.size() - 1);


            milvusClient.buildRAGContent(
                    userId,
                    interviewer.getKnowledgeBaseId(),
                    lastMessage.getContent().getText(),
                    5
            );
            // 2. 生成Prompt
            String prompt = InterviewerPromptGenerator.generatePrompt(interviewer);

            // 3. 调用AI接口
            Flowable<ApplicationResult> aiStream = chatUtil.streamCall(
                    messageList,
                    prompt,
                    ChatUtil.AppType.INTERVIEWER
            );


            // 4. 使用Consumer处理流式输出
            aiStream.blockingSubscribe(data -> {
                ChatOutput chatOutput = new ChatOutput(data.getOutput());
                //System.out.println("content: " + chatOutput.getText());
                //添加验证信息和标识信息
                chatOutput.setUserId(userId);
                chatOutput.setMessageId(messageId);
                System.out.println(chatOutput);
                outputConsumer.accept(chatOutput);
            }

            );

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("处理异常: " + e.getMessage());
        }
    }

    /**
     * 上传文件，并解析文件相关内容
     * @param multipartFile
     * @return
     * @throws IOException
     */
    @Override
    public MessageFileDto convertMessageFile(MultipartFile multipartFile) throws IOException {
        FileInfo fileInfo = FileUploadUtil.uploadFile(multipartFile);

        return fileAnalysisService.analyzeFileInfo(fileInfo);
    }

    @Override
    public boolean saveBranches(List<Branch> branchList) {
        branchRepository.saveAll(branchList);
        return true;
    }


}

package com.sdumagicode.backend.service.impl;

import com.alibaba.dashscope.app.ApplicationResult;
import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.dto.chat.ChatOutput;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.dto.chat.MessageLocalDto;
import com.sdumagicode.backend.entity.CodeSubmission;
import com.sdumagicode.backend.entity.chat.*;
import com.sdumagicode.backend.mapper.ChatMapper;
import com.sdumagicode.backend.mapper.ValuationMapper;
import com.sdumagicode.backend.mapper.mongoRepo.BranchRepository;
import com.sdumagicode.backend.mapper.mongoRepo.ValuationRecordRepository;
import com.sdumagicode.backend.service.ChatService;
import com.sdumagicode.backend.service.InterviewerService;
import com.sdumagicode.backend.util.UserUtils;
import com.sdumagicode.backend.util.chatUtil.ChatUtil;
import com.sdumagicode.backend.util.chatUtil.FileUploadUtil;
import com.sdumagicode.backend.util.chatUtil.InterviewerPromptGenerator;
import com.sdumagicode.backend.util.embeddingUtil.MilvusClient;
import io.reactivex.Flowable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

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

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ValuationRecordRepository valuationRecordRepository;

    @Autowired
    ValuationMapper valuationMapper;

    @Autowired
    InterviewerPromptGenerator interviewerPromptGenerator;

    @Autowired
    InterviewerService interviewerService;

    @Override
    public List<ChatRecords> getChatRecords(ChatRecords chatRecords) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        List<ChatRecords> chatRecordsList = chatMapper.selectChatRecords(userId, chatRecords);
        return chatRecordsList;
    }

    @Override
    public ChatRecords saveChatRecords(ChatRecords chatRecords) {
        chatRecords.setUserId(UserUtils.getCurrentUserByToken().getIdUser());
        if (chatRecords.getChatId() == null) {
            chatRecords.setCreatedAt(LocalDateTime.now());
            chatRecords.setUpdatedAt(LocalDateTime.now());

            // 使用 insert 方法，会自动填充主键
            chatMapper.insertChatRecord(chatRecords);

            // 创建对应的评价表
            List<Valuation> valuations = valuationMapper.selectAll();

            List<ValuationRank> collect = valuations.stream().map((item) -> {
                ValuationRank newRank = new ValuationRank();
                newRank.setRank(0);
                newRank.setValuation(item);
                return newRank;
            }).collect(Collectors.toList());
            ValuationRecord valuationRecord = new ValuationRecord();
            valuationRecord.setChatId(chatRecords.getChatId());
            valuationRecord.setValuationRanks(collect);
            valuationRecordRepository.insert(valuationRecord);

        } else {
            chatRecords.setUpdatedAt(LocalDateTime.now());
            chatMapper.updateChatRecord(chatRecords);
        }
        return chatRecords;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteChatRecords(ChatRecords chatRecords) {
        try {
            // 先删除对应的branch
            List<Branch> branches = branchRepository.findByChatId(chatRecords.getChatId());
            if (branches != null && !branches.isEmpty()) {
                branchRepository.deleteAll(branches);
            }

            // 再删除对应的valuationRecord
            ValuationRecord byChatId = valuationRecordRepository.findByChatId(chatRecords.getChatId());
            if(byChatId != null && byChatId.getValuationRanks() != null && !byChatId.getValuationRanks().isEmpty()){
                valuationRecordRepository.delete(byChatId);
            }


            // 最后删除聊天记录本身
            int result = chatMapper.deleteChatRecord(chatRecords.getChatId());

            if (result <= 0) {
                throw new ServiceException("删除聊天记录失败");
            }

            return true;
        } catch (Exception e) {
            throw new ServiceException("删除聊天记录时发生错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteChatRecords(List<Long> chatIds) {
        try {
            if (chatIds == null || chatIds.isEmpty()) {
                return true;
            }
            
            for (Long chatId : chatIds) {
                // 先删除对应的branch
                List<Branch> branches = branchRepository.findByChatId(chatId);
                if (branches != null && !branches.isEmpty()) {
                    branchRepository.deleteAll(branches);
                }
                
                // 再删除对应的valuationRecord
                ValuationRecord byChatId = valuationRecordRepository.findByChatId(chatId);
                if(byChatId != null && byChatId.getValuationRanks() != null && !byChatId.getValuationRanks().isEmpty()){
                    valuationRecordRepository.delete(byChatId);
                }
                
                // 最后删除聊天记录本身
                int result = chatMapper.deleteChatRecord(chatId);
                if (result <= 0) {
                    throw new ServiceException("批量删除聊天记录失败，ID: " + chatId);
                }
            }
            
            return true;
        } catch (Exception e) {
            throw new ServiceException("批量删除聊天记录时发生错误: " + e.getMessage());
        }
    }

    @Override
    public List<Branch> getAllBranches(ChatRecords chatRecords) {
        List<Branch> byChatId = branchRepository.findByChatId(chatRecords.getChatId());
        return byChatId;
    }

    @Override
    public Flux<ChatOutput> sendMessageAndGetFlux(List<MessageLocal> messageList, String Prompt,
            ChatUtil.AppType appType) {
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
    public Flux<ChatOutput> sendMessageToInterviewerAndGetFlux(List<MessageLocal> messageList,
            Interviewer interviewer) {
        // 对用户最后一条信息进行RAG搜索
        MessageLocal messageLocal = messageList.get(messageList.size() - 1);

        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        milvusClient.buildRAGContent(idUser, interviewer.getKnowledgeBaseId(), messageLocal.getContent().getText(), 5);

        // System.out.println(InterviewerPromptGenerator.generatePrompt(interviewer));
        return sendMessageAndGetFlux(messageList, interviewerPromptGenerator.generatePrompt(interviewer),
                ChatUtil.AppType.INTERVIEWER);
    }

    @Override
    @Async
    public void sendMessageToInterviewer(List<MessageLocal> messageList, Interviewer interviewer, Long userId,
            String messageId, Consumer<ChatOutput> outputConsumer) {
        try {

            Optional<Branch> byId = branchRepository.findById(messageList.get(0).getBranchId());
            if (UserUtils.getCurrentChatId() != null) {
                UserUtils.clearCurrentChatId();
            }
            UserUtils.setCurrentChatId(byId.get().getChatId());
            String ragContent = "";
            // 1. RAG搜索
            if(interviewer.getKnowledgeBaseId() != null){
                MessageLocal lastMessage = messageList.get(messageList.size() - 1);

                 ragContent = milvusClient.buildRAGContent(
                        userId,
                        interviewer.getKnowledgeBaseId(),
                        lastMessage.getContent().getText(),
                        5);



            }

            // 2. 生成Prompt
            String prompt = interviewerPromptGenerator.generatePrompt(interviewer);
            // System.out.println(prompt);

            // 3. 调用AI接口
            Flowable<ApplicationResult> aiStream = chatUtil.streamCall(
                    messageList,
                    prompt,
                    ChatUtil.AppType.INTERVIEWER);

            // 4. 使用Consumer处理流式输出
            aiStream.blockingSubscribe(data -> {
                ChatOutput chatOutput = new ChatOutput(data.getOutput());
                // System.out.println("content: " + chatOutput.getText());
                // 添加验证信息和标识信息
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

    @Override
    @Async
    public void sendMessageToCoder(CodeSubmission codeSubmission, Long userId, Consumer<ChatOutput> outputConsumer) {
        try {

            // 1. 生成Prompt
            String prompt = InterviewerPromptGenerator.generateCoderPrompt(codeSubmission);

            // 2.生成发送内容并组装成messageList
            String text = InterviewerPromptGenerator.generateCodeMessageContent(codeSubmission);
            MessageLocal messageLocal = new MessageLocal();
            // 设置role为user，确保AI能正确处理代码评审请求
            messageLocal.setRole("user");
            Content content = new Content();
            content.setText(text);
            messageLocal.setContent(content);
            List<MessageLocal> messageList = new ArrayList<>();
            messageList.add(messageLocal);

            // 3. 调用AI接口
            Flowable<ApplicationResult> aiStream = chatUtil.streamCall(
                    messageList,
                    prompt,
                    ChatUtil.AppType.CODER);

            // 使用submissionId作为messageId作为messageId
            // 4. 使用Consumer处理流式输出
            aiStream.blockingSubscribe(data -> {
                ChatOutput chatOutput = new ChatOutput(data.getOutput());
                // System.out.println("content: " + chatOutput.getText());
                // 添加验证信息和标识信息
                chatOutput.setUserId(userId);
                chatOutput.setMessageId(String.valueOf(codeSubmission.getId()));
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
     * 
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

    public List<MessageLocal> convertMessageListDto(List<MessageLocalDto> messageLocalDtoList) {

        List<MessageLocal> messageLocalList = messageLocalDtoList.stream().map(item -> {
            // 对有上传文件的处理
            // 当uploadFiles有值时，说明这个信息是第一次发送，尚未执行转化，或者是重新上传文件，覆盖原来的文件
            // 提取文件中的内容分析，获取fileInfoList后保存这条信息
            List<MultipartFile> uploadFiles = item.getUploadFiles();
            MessageLocal messageLocal;
            if (uploadFiles != null && !uploadFiles.isEmpty()) {
                List<FileInfo> files = item.getContent().getFiles();
                for (MultipartFile file : uploadFiles) {
                    try {
                        MessageFileDto messageFileDto = convertMessageFile(file);
                        FileInfo fileInfo = messageFileDto.getFileInfo();
                        fileInfo.setTextContent(messageFileDto.getText());
                        files.add(fileInfo);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new ServiceException("文件分析失败");
                    }
                }
                // 构建新的messageLocal并保存
                Content content = item.getContent();
                content.setFiles(files);
                item.setContent(content);
                messageLocal = item;
                updateMessageLocalWithRepository(messageLocal);
            }
            messageLocal = item;
            return messageLocal;

        }).collect(Collectors.toList());

        return messageLocalList;
    }

    @Override
    public ValuationRecord getValuationRecord(Long chatId) {
        ValuationRecord byChatId = valuationRecordRepository.findByChatId(chatId);
        return byChatId;
    }

    @Transactional
    public void updateMessageLocalWithRepository(MessageLocal updatedMessage) {
        // 1. 先查询出整个Branch文档
        Branch branch = branchRepository.findById(updatedMessage.getBranchId())
                .orElseThrow(() -> new ServiceException("未找到对应的Branch记录"));

        // 2. 找到要更新的MessageLocal
        Optional<MessageLocal> messageLocalOpt = branch.getMessageLocals().stream()
                .filter(msg -> msg.getMessageId().equals(updatedMessage.getMessageId()))
                .findFirst();

        if (messageLocalOpt.isPresent()) {
            MessageLocal messageLocal = messageLocalOpt.get();
            // 3. 更新字段
            messageLocal.setRole(updatedMessage.getRole());
            messageLocal.setInputType(updatedMessage.getInputType());
            messageLocal.setContent(updatedMessage.getContent());
            messageLocal.setTimestamp(updatedMessage.getTimestamp());

            // 4. 保存整个文档
            branchRepository.save(branch);
        } else {
            throw new ServiceException("未找到要更新的MessageLocal记录");
        }
    }

    @Override
    public boolean updateChatTopic(Long chatId, String newTopic) {
        if (chatId == null || newTopic == null || newTopic.trim().isEmpty()) {
            throw new ServiceException("聊天ID或话题名称不能为空");
        }

        // 检查聊天记录是否存在
        ChatRecords existingRecord = chatMapper.selectById(chatId);
        if (existingRecord == null) {
            throw new ServiceException("聊天记录不存在");
        }

        // 检查用户权限
        Long currentUserId = UserUtils.getCurrentUserByToken().getIdUser();
        if (!existingRecord.getUserId().equals(currentUserId)) {
            throw new ServiceException("无权修改此聊天记录");
        }

        // 更新话题
        existingRecord.setTopic(newTopic);
        existingRecord.setUpdatedAt(LocalDateTime.now());
        int result = chatMapper.updateChatRecord(existingRecord);

        return result > 0;
    }

    @Override
    public ChatRecords deepCopy(Long sourceChatId, Long userId) {
        ChatRecords sourceChatRecords = chatMapper.selectById(sourceChatId);
        if(sourceChatRecords == null){
            throw new ServiceException("无效ID");
        }
        String interviewerId = sourceChatRecords.getInterviewerId();
        Interviewer sourceInterviewer = interviewerService.deepCopy(interviewerId, userId);

        ChatRecords chatRecords = new ChatRecords();
        chatRecords.setInterviewerId(sourceInterviewer.getInterviewerId());
        chatRecords.setTopic("用户分享的话题: "+sourceChatRecords.getTopic());
        chatRecords.setCreatedAt(LocalDateTime.now());
        chatRecords.setUpdatedAt(LocalDateTime.now());
        chatRecords.setUserId(userId);
        ChatRecords save = chatMapper.insertChatRecord(chatRecords);

        List<Branch> sourceBranches = branchRepository.findByChatId(chatRecords.getChatId());
        List<Branch> collect = sourceBranches.stream().map((item) -> {
            Branch branch = branchDeepCopy(item, userId, save.getChatId());
            return branch;
        }).collect(Collectors.toList());
        branchRepository.saveAll(collect);
        return save;
    }

    /**
     *
     * @param sourceBranch 待复制的branch
     * @param userId 要复制到的userId
     * @return
     */
    public Branch branchDeepCopy(Branch sourceBranch,Long userId,Long chatId){
        Branch branch = new Branch();
        String uuid = UUID.randomUUID().toString();
        branch.setBranchId(uuid);
        branch.setIndex(sourceBranch.getIndex());
        branch.setChatId(chatId);
        branch.setUserId(userId);
        branch.setParentBranchIndex(sourceBranch.getParentBranchIndex());
        branch.setChildren(sourceBranch.getChildren());



        List<MessageLocal> messageLocals = sourceBranch.getMessageLocals();
        List<MessageLocal> messageLocalList = messageLocals.stream().map((item) -> {
            item.setBranchId(uuid);
            return item;
        }).collect(Collectors.toList());
        branch.setMessageLocals(messageLocalList);
        //暂时不保存,之后保存整个list
        return branch;
    }

}

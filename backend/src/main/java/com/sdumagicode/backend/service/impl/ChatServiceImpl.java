package com.sdumagicode.backend.service.impl;

import com.alibaba.dashscope.app.ApplicationOutput;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sdumagicode.backend.core.result.GlobalResult;
import com.sdumagicode.backend.core.result.GlobalResultGenerator;
import com.sdumagicode.backend.core.service.AbstractService;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

@Service
public class ChatServiceImpl extends AbstractService<ChatRecords> implements ChatService {

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    FileAnalysisService fileAnalysisService;

    @Autowired
    MilvusClient milvusClient;

    @Override
    public List<ChatRecords> getChatRecords(ChatRecords chatRecords) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        LambdaQueryWrapper<ChatRecords> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ChatRecords::getUserId,userId);
        lqw.eq(ChatRecords::getInterviewerId,chatRecords.getInterviewerId());


        return chatMapper.selectByCondition(lqw);
    }

    @Override
    public List<Branch> getAllBranches(ChatRecords chatRecords) {
        return branchRepository.findByChatId(chatRecords.getChatId()+"");
    }

    @Override
    public Flux<GlobalResult<ApplicationOutput>> sendMessageAndGetFlux(List<MessageLocal> messageList, String Prompt) {
        try {
            // 1. 创建工具类实例
            ChatUtil chatUtil = new ChatUtil(
                    ChatUtil.AppType.INTERVIEWER,
                    Prompt);


            // 2. 调用AI接口并转换为Flux流
            Flowable<ApplicationResult> aiStream = chatUtil.streamCall(messageList);


            // 3. 将Flowable转换为Flux并包装成GlobalResult
            return Flux.from(aiStream)
                    .map(result -> {
                        ApplicationOutput output = result.getOutput();
                        return GlobalResultGenerator.genSuccessResult(output);
                    })
                    .onErrorResume(e -> {
                        if (e instanceof ApiException) {
                            return Flux.just(GlobalResultGenerator.genErrorResult("API调用异常: " + e.getMessage()));
                        } else if (e instanceof NoApiKeyException) {
                            return Flux.just(GlobalResultGenerator.genErrorResult("API密钥缺失: " + e.getMessage()));
                        } else if (e instanceof InputRequiredException) {
                            return Flux.just(GlobalResultGenerator.genErrorResult("输入参数缺失: " + e.getMessage()));
                        } else {
                            return Flux.just(GlobalResultGenerator.genErrorResult("系统异常: " + e.getMessage()));
                        }
                    });

        } catch (Exception e) {
            return Flux.just(GlobalResultGenerator.genErrorResult("处理异常: " + e.getMessage()));
        }
    }

    @Override
    public Flux<GlobalResult<ApplicationOutput>> sendMessageToInterviewerAndGetFlux(List<MessageLocal> messageList, Interviewer interviewer) {
        //对用户最后一条信息进行RAG搜索
        MessageLocal messageLocal = messageList.get(messageList.size() - 1);

        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        milvusClient.buildRAGContent(idUser,interviewer.getKnowledgeBaseId(),messageLocal.getContent().getText(),5);


        return sendMessageAndGetFlux(messageList,InterviewerPromptGenerator.generatePrompt(interviewer));
    }

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

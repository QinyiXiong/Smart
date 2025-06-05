package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.entity.chat.AiSettingContent;
import com.sdumagicode.backend.entity.chat.AiSettings;
import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import com.sdumagicode.backend.mapper.AiSettingMapper;
import com.sdumagicode.backend.mapper.mongoRepo.InterviewerRepository;
import com.sdumagicode.backend.service.InterviewerService;
import com.sdumagicode.backend.service.MilvusService;
import com.sdumagicode.backend.util.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InterviewerServiceImpl implements InterviewerService {

    @Autowired
    InterviewerRepository interviewerRepository;
    @Autowired
    AiSettingMapper aiSettingMapper;
    @Autowired
    MilvusService milvusService;

    @Override
    public boolean saveOrUpdateInterviewer(Interviewer interviewer) {
        interviewer.setUserId(UserUtils.getCurrentUserByToken().getIdUser());
        interviewerRepository.save(interviewer);

        return true;
    }

    @Override
    public boolean deleteInterviewer(String interviewerId) {

        interviewerRepository.deleteById(interviewerId);
        return true;
    }

    @Override
    public boolean batchDeleteInterviewers(List<String> interviewerIds) {
        if (interviewerIds == null || interviewerIds.isEmpty()) {
            return true;
        }
        
        try {
            for (String interviewerId : interviewerIds) {
                interviewerRepository.deleteById(interviewerId);
            }
            return true;
        } catch (Exception e) {
            throw new ServiceException("批量删除面试官时发生错误: " + e.getMessage());
        }
    }

    @Override
    public List<Interviewer> findInterviewers() {
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        // 查询当前用户的面试官
        List<Interviewer> allByUserId = interviewerRepository.findAllByUserId(idUser);
        // 查询没有用户ID的面试官
        List<Interviewer> publicInterviewers = interviewerRepository.findAllByUserIdIsNull();
        
        // 合并两个列表
        List<Interviewer> result = new ArrayList<>();
        if (allByUserId != null) {
            result.addAll(allByUserId);
        }
        if (publicInterviewers != null) {
            result.addAll(publicInterviewers);
        }
        
        // 如果用户没有面试官，创建默认面试官
        if (allByUserId == null || allByUserId.isEmpty()){
            Interviewer defaultInterviewer = createDefaultInterviewer(idUser);
            interviewerRepository.save(defaultInterviewer);
            result.add(defaultInterviewer);
        }
        
        return result;
    }

    @Override
    public List<AiSettings> getAllAiSettings() {
        List<AiSettings> aiSettings = aiSettingMapper.selectAll();
        return aiSettings;
    }

    @Override
    public Interviewer findInterviewerById(String interviewerId) {
        Optional<Interviewer> byId = interviewerRepository.findById(interviewerId);
        if(!byId.isPresent()){
            throw new ServiceException("面试官不存在");

        }

        return byId.get();
    }

    /**
     *
     * @param interviewerId 复制的源interviewerId
     * @param userId 要复制到的userId
     * @return
     */
    @Override
    public Interviewer deepCopy(String interviewerId, Long userId) {
        Interviewer interviewer = new Interviewer();
        Optional<Interviewer> byId = interviewerRepository.findById(interviewerId);
        if(!byId.isPresent()){
            throw new ServiceException("id无效");
        }
        Interviewer sourceInterviewer = byId.get();
        //创建一份知识库的复制
        if(sourceInterviewer.getKnowledgeBaseId() != null && !sourceInterviewer.getKnowledgeBaseId().isEmpty()){
            MilvusDatabase milvusDatabase = milvusService.deepCopy(sourceInterviewer.getKnowledgeBaseId(), userId);
            interviewer.setKnowledgeBaseId(milvusDatabase.getKnowledgeBaseId());
        }
        interviewer.setUserId(userId);
        if(userId == 2L){
            interviewer.setName(sourceInterviewer.getName());
        }else{
            interviewer.setName("用户分享的面试官: "+sourceInterviewer.getName());
        }

        interviewer.setCustomPrompt(sourceInterviewer.getCustomPrompt());
        interviewer.setSettingsList(sourceInterviewer.getSettingsList());
        Interviewer save = interviewerRepository.save(interviewer);
        return save;

    }

    Interviewer createDefaultInterviewer(Long userId){
        Interviewer interviewer = new Interviewer();
        interviewer.setUserId(userId);
        interviewer.setName("默认面试官");
        interviewer.setCustomPrompt("");
        List<AiSettings> aiSettings = aiSettingMapper.selectAll();



        List<AiSettingContent> collect = aiSettings.stream().map((item) -> {
            AiSettingContent aiSettingContent = new AiSettingContent();
            aiSettingContent.setExtent(5);
            aiSettingContent.setSettingName(item.getSettingName());
            aiSettingContent.setId(item.getId());
            aiSettingContent.setDescription(item.getDescription());
            return aiSettingContent;
        }).collect(Collectors.toList());

        interviewer.setSettingsList(collect);

        return interviewer;

    }
}

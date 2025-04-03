package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.entity.chat.AiSettings;
import com.sdumagicode.backend.entity.chat.Interviewer;
import com.sdumagicode.backend.mapper.AiSettingMapper;
import com.sdumagicode.backend.mapper.mongoRepo.InterviewerRepository;
import com.sdumagicode.backend.service.InterviewerService;
import com.sdumagicode.backend.util.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewerServiceImpl implements InterviewerService {

    @Autowired
    InterviewerRepository interviewerRepository;
    @Autowired
    AiSettingMapper aiSettingMapper;

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
    public List<Interviewer> findInterviewers() {
        Long idUser = UserUtils.getCurrentUserByToken().getIdUser();
        List<Interviewer> allByUserId = interviewerRepository.findAllByUserId(idUser);
        return allByUserId;
    }

    @Override
    public List<AiSettings> getAllAiSettings() {
        List<AiSettings> aiSettings = aiSettingMapper.selectAll();
        return aiSettings;
    }
}

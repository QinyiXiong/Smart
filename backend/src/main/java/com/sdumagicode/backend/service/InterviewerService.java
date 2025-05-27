package com.sdumagicode.backend.service;

import com.sdumagicode.backend.entity.chat.AiSettings;
import com.sdumagicode.backend.entity.chat.Interviewer;

import java.util.List;

public interface InterviewerService {
    public boolean saveOrUpdateInterviewer(Interviewer interviewer);

    public boolean deleteInterviewer(String interviewerId);

    /**
     * 批量删除面试官
     * @param interviewerIds 面试官ID列表
     * @return 是否删除成功
     */
    public boolean batchDeleteInterviewers(List<String> interviewerIds);

    public List<Interviewer> findInterviewers();

    public List<AiSettings> getAllAiSettings();

    public Interviewer findInterviewerById(String interviewerId);

    public Interviewer deepCopy(String interviewerId,Long userId);
}

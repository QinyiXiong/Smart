package com.sdumagicode.backend.service;

import com.sdumagicode.backend.entity.chat.AiSettings;
import com.sdumagicode.backend.entity.chat.Interviewer;

import java.util.List;

public interface InterviewerService {
    public boolean saveOrUpdateInterviewer(Interviewer interviewer);

    public boolean deleteInterviewer(String interviewerId);

    public List<Interviewer> findInterviewers();

    public List<AiSettings> getAllAiSettings();
}

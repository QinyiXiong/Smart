package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.entity.chat.Interviewer;

public class InterviewerPromptGenerator {


    public static String generatePrompt(Interviewer interviewer){
        return interviewer.getPromptTemplate();
    }
}

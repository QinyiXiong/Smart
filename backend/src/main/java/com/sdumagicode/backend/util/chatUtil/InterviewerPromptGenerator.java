package com.sdumagicode.backend.util.chatUtil;

import com.sdumagicode.backend.entity.chat.Interviewer;

public class InterviewerPromptGenerator {

    private static final String PROMPT_TEMPLATE = "text";

    public static String generatePrompt(Interviewer interviewer){


        return interviewer.getCustomPrompt() + PROMPT_TEMPLATE;


    }

    public static String generateCoderPrompt(){
        return "";
    }
}

package com.sdumagicode.backend;

import com.sdumagicode.backend.service.InterviewerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class InterviewerTest {
    @Autowired
    InterviewerService interviewerService;
    @Test
    public void deepCopyTest(){
        interviewerService.deepCopy("6832ff119020f03d3cdec893",65002L);
    }
}

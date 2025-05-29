package com.sdumagicode.backend.mapper.mongoRepo;

import com.sdumagicode.backend.entity.chat.Interviewer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InterviewerRepository extends MongoRepository<Interviewer,String> {

    List<Interviewer> findAllByUserId(Long userId);
    
    List<Interviewer> findAllByUserIdIsNull();
}

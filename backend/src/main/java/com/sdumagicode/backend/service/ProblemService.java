package com.sdumagicode.backend.service;

import com.sdumagicode.backend.dto.ProblemDTO;

import java.util.List;
import java.util.Map;

public interface ProblemService {
    ProblemDTO getProblemById(Long id);
    void addProblem(ProblemDTO problemDTO);
    void updateProblem(ProblemDTO problemDTO);
    void deleteProblem(Long id);
    
    List<ProblemDTO> selectProblems(String difficulty, String category);
    
    List<ProblemDTO> selectProblemsByCategory(String category);
    
    List<ProblemDTO> selectProblemsByDifficulty(String difficulty);
    
    Long postProblem(ProblemDTO problemDTO);
    
    Integer delete(Long id);
    
    ProblemDTO selectProblemById(Long id);

    List<String> getAllTags();
    
    Map<String, Object> getStatistics();
}
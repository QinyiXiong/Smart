package com.sdumagicode.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProblemDTO {
    private Long id;
    private String problemCode;
    private String title;
    private String description;
    private String inputDescription;
    private String outputDescription;
    private String difficulty;
    private Integer timeLimit;
    private Integer memoryLimit;
    private List<String> tags;
    private String sampleInput;
    private String sampleOutput;
    private String testCases;
    private String hints;
    private String source;
    private Integer submitCount;
    private Integer acceptCount;
    private Double acceptanceRate;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Integer visible;
}
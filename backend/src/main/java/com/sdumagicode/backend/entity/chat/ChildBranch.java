package com.sdumagicode.backend.entity.chat;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
public class ChildBranch {
    @Id
    private String branchId;
    private String tag;

}

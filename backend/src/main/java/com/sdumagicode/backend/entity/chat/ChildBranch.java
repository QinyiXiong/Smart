package com.sdumagicode.backend.entity.chat;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "branches")
public class ChildBranch {
    @Id
    private String id;

    private String branchId;
    private String conversationId;
    private String userId;

    private List<Message> messages;
    private String parentBranchId;
    private List<ChildBranch> children;
}

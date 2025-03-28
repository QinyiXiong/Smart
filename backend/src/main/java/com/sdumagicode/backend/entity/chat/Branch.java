package com.sdumagicode.backend.entity.chat;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 用于保存聊天的详细内容以及分支情况
 */
@Data
@Document(collection = "branch")
public class Branch {
    @Id
    private String id;

    private String branchId;
    private String chatId;
    private String userId;

    private List<MessageLocal> messageLocals;
    private String parentBranchId;
    private List<ChildBranch> children;

}

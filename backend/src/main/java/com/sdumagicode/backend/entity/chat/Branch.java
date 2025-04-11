package com.sdumagicode.backend.entity.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 用于保存聊天的详细内容以及分支情况
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "branch")
public class Branch {
    @Id
    private String branchId;

    private Integer index;

    private Long chatId;
    private Long userId;

    @JsonProperty("messageLocals")
    private List<MessageLocal> messageLocals;
    private String parentBranchIndex;
    private List<ChildBranch> children;

}

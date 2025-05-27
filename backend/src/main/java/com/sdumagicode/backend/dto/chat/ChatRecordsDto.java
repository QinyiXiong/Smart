package com.sdumagicode.backend.dto.chat;

import com.sdumagicode.backend.entity.chat.Branch;
import com.sdumagicode.backend.entity.chat.ChatRecords;
import com.sdumagicode.backend.entity.chat.Interviewer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRecordsDto extends ChatRecords {


    private Interviewer interviewer;

    private List<Branch> branchList;
}

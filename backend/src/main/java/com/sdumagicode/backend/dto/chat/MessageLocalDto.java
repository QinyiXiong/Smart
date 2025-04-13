package com.sdumagicode.backend.dto.chat;

import com.sdumagicode.backend.entity.chat.MessageLocal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageLocalDto extends MessageLocal {
    //对于第一次上传，尚未生成fileInfo
    private List<MultipartFile> uploadFiles;

    public MessageLocalDto(MessageLocal messageLocal){
        setContent(messageLocal.getContent());
        setBranchId(messageLocal.getBranchId());
        setInputType(messageLocal.getInputType());
        setContent(messageLocal.getContent());
        setTimestamp(messageLocal.getTimestamp());
        setMessageId(messageLocal.getMessageId());
        setRole(messageLocal.getRole());
    }
}

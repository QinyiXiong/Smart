package com.sdumagicode.backend.entity.milvus;


import com.sdumagicode.backend.entity.chat.FileInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import javax.persistence.Id;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MilvusFile {

    //这里的MilvusFile对应一个文件，milvus中的一个chunk是文件的一部分
    @Id
    private String milvusFileId = new ObjectId().toString(); // 自动生成

    private FileInfo fileInfo;
    private List<KnowledgeRecord> knowledgeRecords;

}

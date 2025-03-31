package com.sdumagicode.backend.entity.milvus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Id;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("milvus_database")
public class MilvusDatabase {
    @Id
    private String knowledgeBaseId;

    private String databaseName;

    private Long userId;

    private List<MilvusFile> fileList;
}

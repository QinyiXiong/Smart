package com.sdumagicode.backend.service;

import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import com.sdumagicode.backend.entity.milvus.MilvusFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MilvusService {
    public boolean createMilvusDatabase(MilvusDatabase milvusDatabase);

    public List<MilvusDatabase> getMilvusBases();

    public List<MilvusFile> getMilvusFileList(String knowledgeBaseId);

    public boolean deleteMilvusDatabase(String knowledgeBaseId);

    public boolean uploadFile(String knowledgeBaseId,List<MultipartFile> fileList);

    public boolean deleteMilvusFilse(String knowledgeBaseId,String milvusFileId) throws IOException;

    public MilvusDatabase deepCopyForUser(String knowledgeBaseId);

    public MilvusDatabase deepCopy(String knowledgeBaseId,Long userId);

    public MilvusDatabase deepCopyForSnapshot(String knowledgeBaseId);

}

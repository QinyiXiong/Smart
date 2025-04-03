package com.sdumagicode.backend.service.impl;

import com.sdumagicode.backend.core.exception.ServiceException;
import com.sdumagicode.backend.dto.chat.MessageFileDto;
import com.sdumagicode.backend.entity.chat.FileInfo;
import com.sdumagicode.backend.entity.milvus.KnowledgeRecord;
import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import com.sdumagicode.backend.entity.milvus.MilvusFile;
import com.sdumagicode.backend.mapper.mongoRepo.MilvusDatabaseRepository;
import com.sdumagicode.backend.service.MilvusService;
import com.sdumagicode.backend.util.UserUtils;
import com.sdumagicode.backend.util.chatUtil.FileUploadUtil;
import com.sdumagicode.backend.util.embeddingUtil.MilvusClient;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MilvusServiceImpl implements MilvusService {
    @Autowired
    MilvusDatabaseRepository milvusDatabaseRepository;

    @Autowired
    FileAnalysisService fileAnalysisService;

    @Autowired
    MilvusClient milvusClient;


    @Override
    public boolean createMilvusDatabase(MilvusDatabase milvusDatabase) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();

        milvusDatabase.setUserId(userId);
        milvusDatabase.setFileList(new ArrayList<MilvusFile>());
        MilvusDatabase save = milvusDatabaseRepository.save(milvusDatabase);

        //创建对应的milvus数据库
        milvusClient.createCollection(userId, save.getKnowledgeBaseId());

        return true;
    }

    @Override
    public List<MilvusDatabase> getMilvusBases() {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();
        List<MilvusDatabase> milvusDatabasesByUserId = milvusDatabaseRepository.findMilvusDatabasesByUserId(userId);
        return milvusDatabasesByUserId;
    }

    @Override
    public List<MilvusFile> getMilvusFileList(String knowledgeBaseId) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();

        MilvusDatabase milvusDatabaseByKnowledgeBaseIdAAndUserId = milvusDatabaseRepository.findMilvusDatabaseByKnowledgeBaseIdAndUserId(knowledgeBaseId, userId);
        return milvusDatabaseByKnowledgeBaseIdAAndUserId.getFileList();
    }

    @Override
    public boolean deleteMilvusDatabase(String knowledgeBaseId) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();

        MilvusDatabase milvusDatabaseByKnowledgeBaseIdAAndUserId = milvusDatabaseRepository.findMilvusDatabaseByKnowledgeBaseIdAndUserId(knowledgeBaseId, userId);
        if(milvusDatabaseByKnowledgeBaseIdAAndUserId == null){
            throw new ServiceException("未找到对应知识库");
        }
        milvusDatabaseRepository.delete(milvusDatabaseByKnowledgeBaseIdAAndUserId);
        milvusClient.dropCollection(userId,knowledgeBaseId);
        return true;
    }

    @Override
    public boolean uploadFile(String knowledgeBaseId,List<MultipartFile> fileList) {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();

        MilvusDatabase milvusDatabaseByKnowledgeBaseIdAAndUserId = milvusDatabaseRepository.findMilvusDatabaseByKnowledgeBaseIdAndUserId(knowledgeBaseId, userId);
        if(milvusDatabaseByKnowledgeBaseIdAAndUserId == null){
            throw new ServiceException("未找到对应知识库");
        }
        List<MilvusFile> milvusFileList = fileList.stream().map(item -> {
            //上传文件，获取fileInfo
            FileInfo fileInfo;
            try {
                fileInfo = FileUploadUtil.uploadFile(item);
            } catch (IOException e) {
                throw new ServiceException("文件上传失败");
            }
            //通过fileInfo解析文件,得到文本形式的内容
            String text = fileAnalysisService.analyzeFile(fileInfo).getText();
            MilvusFile milvusFile = new MilvusFile();
            milvusFile.setFileInfo(fileInfo);
            List<String> splitedDocuments = milvusClient.splitDocument(text);
            List<KnowledgeRecord> knowledgeRecords = IntStream.range(0, splitedDocuments.size())
                    .mapToObj(chunkIndex -> {
                        KnowledgeRecord knowledgeRecord = new KnowledgeRecord();
                        knowledgeRecord.generateRecordId();
                        knowledgeRecord.setFileId(fileInfo.getFileId());
                        knowledgeRecord.setChunkText(splitedDocuments.get(chunkIndex));
                        knowledgeRecord.setChunkIndex(chunkIndex);
                        knowledgeRecord.setFileName(fileInfo.getFileName());

                        milvusClient.insertMilvus(knowledgeRecord, userId, knowledgeBaseId);
                        return knowledgeRecord;
                    })
                    .collect(Collectors.toList());
            milvusFile.setKnowledgeRecords(knowledgeRecords);
            return milvusFile;
        }).collect(Collectors.toList());
        milvusFileList.addAll(milvusDatabaseByKnowledgeBaseIdAAndUserId.getFileList());
        milvusDatabaseByKnowledgeBaseIdAAndUserId.setFileList(milvusFileList);
        milvusDatabaseRepository.save(milvusDatabaseByKnowledgeBaseIdAAndUserId);

        return true;
    }

    @Override
    public boolean deleteMilvusFilse(String knowledgeBaseId, String milvusFileId) throws IOException {
        Long userId = UserUtils.getCurrentUserByToken().getIdUser();

        MilvusDatabase milvusDatabaseByKnowledgeBaseIdAAndUserId = milvusDatabaseRepository.findMilvusDatabaseByKnowledgeBaseIdAndUserId(knowledgeBaseId, userId);
        if(milvusDatabaseByKnowledgeBaseIdAAndUserId == null){
            throw new ServiceException("未找到对应知识库");
        }
        MilvusFile milvusFile = milvusDatabaseByKnowledgeBaseIdAAndUserId.getFileList().stream()
                .filter(file -> milvusFileId.equals(file.getMilvusFileId()))
                .findFirst()
                .orElse(null);
        //首先删除milvus中的相关数据

        if(milvusFile != null){
            List<Long> collect = milvusFile.getKnowledgeRecords().stream().map(item -> {
                return item.getRecordId();
            }).collect(Collectors.toList());
            if(!collect.isEmpty()){
                milvusClient.deleteBatch(collect,userId,knowledgeBaseId);
            }
        }



        //再删除mongo中的对应数据
        List<MilvusFile> updatedFileList = milvusDatabaseByKnowledgeBaseIdAAndUserId.getFileList().stream()
                .filter(file -> !file.getMilvusFileId().equals(milvusFileId))
                .collect(Collectors.toList());

        milvusDatabaseByKnowledgeBaseIdAAndUserId.setFileList(updatedFileList);
        milvusDatabaseRepository.save(milvusDatabaseByKnowledgeBaseIdAAndUserId);

        //最后删除本地的数据
        FileUploadUtil.deleteFileByUrl(milvusFile.getFileInfo().getUrl());
        return true;
    }
}

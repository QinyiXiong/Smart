package com.sdumagicode.backend.mapper.mongoRepo;

import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilvusDatabaseRepository extends MongoRepository<MilvusDatabase,String> {

    List<MilvusDatabase> findMilvusDatabasesByUserId(Long UserId);

    MilvusDatabase findMilvusDatabaseByKnowledgeBaseIdAndUserId(String knowledgeBaseId,Long userId);

    MilvusDatabase findMilvusDatabaseByKnowledgeBaseId(String knowledgebaseId);

    @Query("{'fileList.milvusFileId': ?0}")
    MilvusDatabase findByMilvusFileId(String milvusFileId);
}

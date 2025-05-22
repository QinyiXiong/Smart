package com.sdumagicode.backend;

import com.sdumagicode.backend.entity.milvus.MilvusDatabase;
import com.sdumagicode.backend.service.MilvusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MilvusTest {

    @Autowired
    MilvusService milvusService;

    @Test
    public void deepCopyTest(){
        MilvusDatabase milvusDatabase = milvusService.deepCopyForUser("682dd6c6ea848a69462be171");
        System.out.println(milvusDatabase.getDatabaseName());
    }
}

package com.sdumagicode.backend;

import com.sdumagicode.backend.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ForestApplicationTests {
    @Autowired
    UserMapper userMapper;
    @Test
    void contextLoads() {
        userMapper.updatePasswordByEmail("admin@rymcu.com","123456");
    }

}

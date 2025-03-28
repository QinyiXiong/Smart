package com.sdumagicode.backend;

import com.sdumagicode.backend.mapper.UserMapper;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;

@SpringBootTest
class ForestApplicationTests {

    @Test
    void contextLoads() {


// 生成一个符合 HS256 要求的 256 位（32 字节）密钥
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

// 存储密钥（Base64 编码）
        String base64Key = java.util.Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated Key: " + base64Key);  // 示例：`k4GvV8d3...`（44 字符）

    }

}

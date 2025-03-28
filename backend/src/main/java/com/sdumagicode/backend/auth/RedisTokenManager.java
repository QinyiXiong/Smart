package com.sdumagicode.backend.auth;


import com.sdumagicode.backend.handler.event.AccountEvent;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 通过Redis存储和验证token的实现类
 *
 * @author ScienJus
 * @date 2015/7/31.
 */
@Component
public class RedisTokenManager implements TokenManager {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 生成TOKEN
     */
//    @Override
//    public String createToken(String id) {
//        //使用 account 作为源 token
//        String token = Jwts.builder().setId(id).setSubject(id).setIssuedAt(new Date()).signWith(SignatureAlgorithm.HS256, JwtConstants.JWT_SECRET).compact();
//        //存储到 redis 并设置过期时间
//        redisTemplate.boundValueOps(id).set(token, JwtConstants.TOKEN_EXPIRES_MINUTE, TimeUnit.MINUTES);
//        return token;
//    }
    @Override
    public String createToken(String id) {
        // 1. 检查并确保密钥是 256 位（32 字节）
        byte[] keyBytes = JwtConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 256 bits (32 bytes)");
        }

        // 2. 使用 Keys.hmacShaKeyFor() 转换密钥
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        // 3. 生成 Token（使用安全的密钥）
        String token = Jwts.builder()
                .setId(id)
                .setSubject(id)
                .setIssuedAt(new Date())
                .signWith(key, SignatureAlgorithm.HS256)  // 使用 SecretKey 签名
                .compact();
        // 4. 存储到 Redis 并设置过期时间
        redisTemplate.boundValueOps(id).set(token, JwtConstants.TOKEN_EXPIRES_MINUTE, TimeUnit.MINUTES);

        return token;
    }
    @Override
    public TokenModel getToken(String token, String account) {
        return new TokenModel(account, token);
    }

    @Override
    public boolean checkToken(TokenModel model) {
        if (model == null) {
            return false;
        }
        String token = redisTemplate.boundValueOps(model.getUsername()).get();
        if (token == null || !token.equals(model.getToken())) {
            return false;
        }
        StringBuilder key = new StringBuilder();
        key.append(JwtConstants.LAST_ONLINE).append(model.getUsername());
        String result = redisTemplate.boundValueOps(key.toString()).get();
        if (StringUtils.isBlank(result)) {
            // 更新最后在线时间
            applicationEventPublisher.publishEvent(new AccountEvent(model.getUsername()));
            redisTemplate.boundValueOps(key.toString()).set(LocalDateTime.now().toString(), JwtConstants.LAST_ONLINE_EXPIRES_MINUTE, TimeUnit.MINUTES);
        }
        return true;
    }

    @Override
    public void deleteToken(String account) {
        redisTemplate.delete(account);
    }
}

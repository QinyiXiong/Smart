package com.sdumagicode.backend.util;

import com.sdumagicode.backend.auth.JwtConstants;
import com.sdumagicode.backend.auth.TokenManager;
import com.sdumagicode.backend.auth.TokenModel;
import com.sdumagicode.backend.dto.TokenUser;
import com.sdumagicode.backend.entity.User;
import com.sdumagicode.backend.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.UnauthenticatedException;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author ronger
 */
public class UserUtils {

    private static final UserMapper userMapper = SpringContextHolder.getBean(UserMapper.class);
    private static final TokenManager tokenManager = SpringContextHolder.getBean(TokenManager.class);

    private static final ThreadLocal<Long> currentChatId = new ThreadLocal<>();

    private static final SecretKey key = Keys.hmacShaKeyFor(JwtConstants.JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    /**
     * 通过token获取当前用户的信息
     *
     * @return
     */
    public static User getCurrentUserByToken() {
        HttpServletRequest request = ContextHolderUtils.getRequest();
        String authHeader = request.getHeader(JwtConstants.AUTHORIZATION);
        if (authHeader == null) {
            throw new UnauthenticatedException();
        }
        // 验证token
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)  // 设置密钥
                    .build()             // 构建解析器
                    .parseClaimsJws(authHeader)  // 解析 JWT
                    .getBody();
        } catch (final SignatureException e) {
            throw new UnauthenticatedException();
        }
        Object account = claims.getId();
        if (StringUtils.isNotBlank(Objects.toString(account, ""))) {
            TokenModel model = tokenManager.getToken(authHeader, account.toString());
            if (tokenManager.checkToken(model)) {
                User user = userMapper.selectByAccount(account.toString());
                if (Objects.nonNull(user)) {
                    return user;
                }
            }
        }
        throw new UnauthenticatedException();
    }

    public static TokenUser getTokenUser(String token) {
        if (StringUtils.isNotBlank(token)) {
            // 验证token
            Claims claims;
            try {

                claims = Jwts.parserBuilder()
                        .setSigningKey(key)  // 设置密钥
                        .build()             // 构建解析器
                        .parseClaimsJws(token)  // 解析 JWT
                        .getBody();
            } catch (final SignatureException e) {
                throw new UnauthenticatedException();
            }
            Object account = claims.getId();
            if (StringUtils.isNotBlank(Objects.toString(account, ""))) {
                TokenModel model = tokenManager.getToken(token, account.toString());
                if (tokenManager.checkToken(model)) {
                    User user = userMapper.selectByAccount(account.toString());
                    if (Objects.nonNull(user)) {
                        TokenUser tokenUser = new TokenUser();
                        BeanCopierUtil.copy(user, tokenUser);
                        tokenUser.setAccount(user.getEmail());
                        tokenUser.setToken(token);
                        return tokenUser;
                    }
                }
            }
        }
        throw new UnauthenticatedException();
    }


    public static boolean isAdmin(String email) {
        return userMapper.hasAdminPermission(email);
    }

    /**
     * 设置当前请求的 chatId
     */
    public static void setCurrentChatId(Long chatId) {
        currentChatId.set(chatId);
    }
    /**
     * 获取当前请求的 chatId
     */
    public static Long getCurrentChatId() {
        return currentChatId.get();
    }
    /**
     * 清除当前请求的 chatId（防止内存泄漏）
     */
    public static void clearCurrentChatId() {
        currentChatId.remove();
    }
}

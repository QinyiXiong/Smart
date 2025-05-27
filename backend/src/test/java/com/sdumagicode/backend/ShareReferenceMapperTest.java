package com.sdumagicode.backend;

import com.sdumagicode.backend.entity.Article;
import com.sdumagicode.backend.entity.ShareReference;
import com.sdumagicode.backend.mapper.ArticleMapper;
import com.sdumagicode.backend.mapper.ShareReferenceMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional // 确保测试后数据回滚，不影响数据库
public class ShareReferenceMapperTest {

    @Autowired
    private ShareReferenceMapper shareReferenceMapper;
    
    @Autowired
    private ArticleMapper articleMapper;
    
    /**
     * 测试通过参数插入ShareReference
     */
    @Test
    public void testInsertShareReference() {
        // 首先查询一篇存在的文章
        Article article = articleMapper.selectByPrimaryKey(1L);
        Assertions.assertNotNull(article, "测试前提：数据库中应该存在ID为1的文章");
        
        // 插入ShareReference
        Integer type = 1; // 非空
        Long chatId = 100L;
        String interviewerId = "interviewer123";
        Long articleId = article.getIdArticle(); // 非空
        
        Integer result = shareReferenceMapper.insertShareReference(type, chatId, interviewerId, articleId);
        Assertions.assertEquals(1, result, "插入应该成功，并返回1");
        
        // 验证插入结果
        List<ShareReference> references = shareReferenceMapper.selectShareReferencesByArticleId(articleId);
        Assertions.assertFalse(references.isEmpty(), "应该能查询到刚插入的记录");
        
        // 验证查询到的记录中包含我们插入的数据
        boolean found = references.stream()
                .anyMatch(ref -> ref.getType().equals(type) && 
                        (ref.getChatId() == null ? chatId == null : ref.getChatId().equals(chatId)) &&
                        (ref.getInterviewerId() == null ? interviewerId == null : ref.getInterviewerId().equals(interviewerId)));
        Assertions.assertTrue(found, "查询结果中应包含刚插入的记录");
    }
    
    /**
     * 测试使用对象插入ShareReference
     */
    @Test
    public void testInsertShareReferenceByObject() {
        // 首先查询一篇存在的文章
        Article article = articleMapper.selectByPrimaryKey(1L);
        Assertions.assertNotNull(article, "测试前提：数据库中应该存在ID为1的文章");
        
        // 创建ShareReference对象
        ShareReference shareReference = new ShareReference();
        shareReference.setType(2); // 非空
        shareReference.setChatId(200L);
        shareReference.setInterviewerId("interviewer456");
        
        // 设置关联的文章
        shareReference.setArticle(article); // 非空
        
        // 插入ShareReference
        Integer result = shareReferenceMapper.insertShareReferenceByObject(shareReference);
        Assertions.assertEquals(1, result, "插入应该成功，并返回1");
        
        // 验证插入结果
        List<ShareReference> references = shareReferenceMapper.selectShareReferencesByArticleId(article.getIdArticle());
        Assertions.assertFalse(references.isEmpty(), "应该能查询到刚插入的记录");
        
        // 验证查询到的记录中包含我们插入的数据
        boolean found = references.stream()
                .anyMatch(ref -> ref.getType().equals(shareReference.getType()) && 
                        (ref.getChatId() == null ? shareReference.getChatId() == null : ref.getChatId().equals(shareReference.getChatId())) &&
                        (ref.getInterviewerId() == null ? shareReference.getInterviewerId() == null : ref.getInterviewerId().equals(shareReference.getInterviewerId())));
        Assertions.assertTrue(found, "查询结果中应包含刚插入的记录");
    }
    
    /**
     * 测试删除ShareReference
     */
    @Test
    public void testDeleteShareReferenceById() {
        // 首先插入一条记录
        Article article = articleMapper.selectByPrimaryKey(1L);
        Assertions.assertNotNull(article, "测试前提：数据库中应该存在ID为1的文章");
        
        Integer type = 3; // 非空
        Long chatId = 300L;
        String interviewerId = "interviewer789";
        Long articleId = article.getIdArticle(); // 非空
        
        Integer insertResult = shareReferenceMapper.insertShareReference(type, chatId, interviewerId, articleId);
        Assertions.assertEquals(1, insertResult, "插入应该成功，并返回1");
        
        // 查询插入的记录
        List<ShareReference> references = shareReferenceMapper.selectShareReferencesByArticleId(articleId);
        Assertions.assertFalse(references.isEmpty(), "应该能查询到刚插入的记录");
        
        // 获取插入记录的ID
        Long idShare = references.stream()
                .filter(ref -> ref.getType().equals(type) && 
                        (ref.getChatId() == null ? chatId == null : ref.getChatId().equals(chatId)) &&
                        (ref.getInterviewerId() == null ? interviewerId == null : ref.getInterviewerId().equals(interviewerId)))
                .findFirst()
                .map(ShareReference::getIdShare)
                .orElse(null);
        
        Assertions.assertNotNull(idShare, "应该能获取到插入记录的ID");
        
        // 删除记录
        Integer deleteResult = shareReferenceMapper.deleteShareReferenceById(idShare);
        Assertions.assertEquals(1, deleteResult, "删除应该成功，并返回1");
        
        // 验证删除结果
        ShareReference deletedRef = shareReferenceMapper.selectByPrimaryKey(idShare);
        Assertions.assertNull(deletedRef, "删除后应该查询不到该记录");
    }
    
    /**
     * 测试查询ShareReference
     */
    @Test
    public void testSelectShareReferencesByArticleId() {
        // 首先插入两条记录
        Article article = articleMapper.selectByPrimaryKey(1L);
        Assertions.assertNotNull(article, "测试前提：数据库中应该存在ID为1的文章");
        
        Long articleId = article.getIdArticle();
        
        // 插入第一条记录
        Integer type1 = 4;
        Long chatId1 = 400L;
        String interviewerId1 = "interviewer_test_1";
        Integer insertResult1 = shareReferenceMapper.insertShareReference(type1, chatId1, interviewerId1, articleId);
        Assertions.assertEquals(1, insertResult1, "第一条记录插入应该成功");
        
        // 插入第二条记录
        Integer type2 = 5;
        Long chatId2 = 500L;
        String interviewerId2 = "interviewer_test_2";
        Integer insertResult2 = shareReferenceMapper.insertShareReference(type2, chatId2, interviewerId2, articleId);
        Assertions.assertEquals(1, insertResult2, "第二条记录插入应该成功");
        
        // 查询记录
        List<ShareReference> references = shareReferenceMapper.selectShareReferencesByArticleId(articleId);
        
        // 验证查询结果
        Assertions.assertTrue(references.size() >= 2, "应该至少能查询到2条记录");
        
        // 验证查询到的记录中包含我们插入的两条数据
        boolean foundFirst = references.stream()
                .anyMatch(ref -> ref.getType().equals(type1) && 
                        (ref.getChatId() == null ? chatId1 == null : ref.getChatId().equals(chatId1)) &&
                        (ref.getInterviewerId() == null ? interviewerId1 == null : ref.getInterviewerId().equals(interviewerId1)));
        
        boolean foundSecond = references.stream()
                .anyMatch(ref -> ref.getType().equals(type2) && 
                        (ref.getChatId() == null ? chatId2 == null : ref.getChatId().equals(chatId2)) &&
                        (ref.getInterviewerId() == null ? interviewerId2 == null : ref.getInterviewerId().equals(interviewerId2)));
        
        Assertions.assertTrue(foundFirst && foundSecond, "查询结果中应包含刚插入的两条记录");
    }
    
    /**
     * 测试非空约束 - type为空
     */
    @Test
    public void testInsertWithNullType() {
        // 首先查询一篇存在的文章
        Article article = articleMapper.selectByPrimaryKey(1L);
        Assertions.assertNotNull(article, "测试前提：数据库中应该存在ID为1的文章");
        
        // 插入ShareReference，type为null
        Integer type = null; // 违反非空约束
        Long chatId = 600L;
        String interviewerId = "interviewer_null_test";
        Long articleId = article.getIdArticle();
        
        // 断言插入会失败
        Assertions.assertThrows(Exception.class, () -> {
            shareReferenceMapper.insertShareReference(type, chatId, interviewerId, articleId);
        }, "插入时type为null应该抛出异常");
    }
    
    /**
     * 测试非空约束 - article_id为空
     */
    @Test
    public void testInsertWithNullArticleId() {
        // 插入ShareReference，articleId为null
        Integer type = 6;
        Long chatId = 700L;
        String interviewerId = "interviewer_null_test";
        Long articleId = null; // 违反非空约束
        
        // 断言插入会失败
        Assertions.assertThrows(Exception.class, () -> {
            shareReferenceMapper.insertShareReference(type, chatId, interviewerId, articleId);
        }, "插入时articleId为null应该抛出异常");
    }
} 
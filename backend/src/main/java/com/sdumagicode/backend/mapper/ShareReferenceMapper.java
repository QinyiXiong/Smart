package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.entity.ShareReference;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章分享引用Mapper接口
 */
@org.apache.ibatis.annotations.Mapper
public interface ShareReferenceMapper extends Mapper<ShareReference> {

    /**
     * 根据文章ID查询分享引用列表
     *
     * @param articleId
     * @return
     */
    List<ShareReference> selectShareReferencesByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据分享ID删除分享引用
     *
     * @param idShare
     * @return
     */
    Integer deleteShareReferenceById(@Param("idShare") Long idShare);
    
    /**
     * 插入文章分享引用
     *
     * @param type 类型
     * @param chatId 聊天ID
     * @param interviewerId 面试官ID
     * @param articleId 文章ID
     * @return
     */
    Integer insertShareReference(@Param("type") Integer type, @Param("chatId") Long chatId, 
                                @Param("interviewerId") String interviewerId, @Param("articleId") Long articleId);
    
    /**
     * 插入文章分享引用（使用ShareReference对象）
     *
     * @param shareReference 分享引用对象
     * @return
     */
    Integer insertShareReferenceByObject(@Param("shareReference") ShareReference shareReference);
    
    /**
     * 批量插入文章分享引用
     *
     * @param shareReferences 分享引用对象列表
     * @return 插入的记录数
     */
    Integer batchInsertShareReferences(@Param("list") List<ShareReference> shareReferences);

    /**
     * 根据文章ID删除分享引用
     *
     * @param articleId 文章ID
     * @return
     */
    Integer deleteByArticleId(@Param("articleId") Long articleId);
} 
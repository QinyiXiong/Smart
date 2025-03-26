package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.entity.ArticleThumbsUp;
import org.apache.ibatis.annotations.Param;

/**
 * @author ronger
 */
public interface ArticleThumbsUpMapper extends Mapper<ArticleThumbsUp> {
    /**
     * 更新文章点赞数
     *
     * @param idArticle
     * @param thumbsUpNumber
     * @return
     */
    Integer updateArticleThumbsUpNumber(@Param("idArticle") Long idArticle, @Param("thumbsUpNumber") Integer thumbsUpNumber);
}

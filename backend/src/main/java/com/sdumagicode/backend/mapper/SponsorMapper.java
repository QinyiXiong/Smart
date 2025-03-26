package com.sdumagicode.backend.mapper;

import com.sdumagicode.backend.core.mapper.Mapper;
import com.sdumagicode.backend.entity.Sponsor;
import org.apache.ibatis.annotations.Param;

/**
 * @author ronger
 */
public interface SponsorMapper extends Mapper<Sponsor> {
    /**
     * 更新文章赞赏数
     *
     * @param idArticle
     * @return
     */
    Integer updateArticleSponsorCount(@Param("idArticle") Long idArticle);
}

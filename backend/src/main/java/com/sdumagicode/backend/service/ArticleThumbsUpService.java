package com.sdumagicode.backend.service;

import com.sdumagicode.backend.core.service.Service;
import com.sdumagicode.backend.entity.ArticleThumbsUp;

/**
 * 点赞
 *
 * @author ronger
 */
public interface ArticleThumbsUpService extends Service<ArticleThumbsUp> {
    /**
     * 点赞
     *
     * @param articleThumbsUp
     * @return
     */
    int thumbsUp(ArticleThumbsUp articleThumbsUp);
}

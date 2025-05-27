package com.sdumagicode.backend.entity;


import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * @author ronger
 */
@Data
@Table(name = "forest_article_content")
public class ArticleContent {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long idArticle;

    private String articleContent;

    private String articleContentHtml;

    private Date createdTime;

    private Date updatedTime;

}

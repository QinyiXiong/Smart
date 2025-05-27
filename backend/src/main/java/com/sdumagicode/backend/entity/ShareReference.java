package com.sdumagicode.backend.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Table(name = "forest_article_ai_share")
public class ShareReference {
    @Id
    @GeneratedValue(generator = "JDBC")
    @Column(name = "id")
    private Long idShare;

    //0为聊天记录,1为面试官
    private Integer type;

    private Long chatId;

    private String interviewerId;

    @ManyToOne
    @JoinColumn(name = "article_id",referencedColumnName = "id")
    private Article article;


}

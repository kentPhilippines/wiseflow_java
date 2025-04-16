package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Integer categoryId;
    private String categoryName;
    private String source;
    private String author;
    private String url;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isOriginal; // 是否为原创文章
    private Long originalArticleId; // 如果是改写文章，关联原文章ID
} 
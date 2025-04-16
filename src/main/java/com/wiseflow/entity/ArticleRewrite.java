package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_article_rewrite")
public class ArticleRewrite {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 原文章ID
     */
    private Long originalArticleId;
    
    /**
     * 改写后的标题
     */
    private String title;
    
    /**
     * 改写后的内容
     */
    private String content;
    
    /**
     * 原创度评分（0-100）
     */
    private Integer originalityScore;
    /**
     * 处理状态：
     * PENDING - 待处理
     * PROCESSING - 处理中
     * COMPLETED - 已完成 
     * FAILED - 失败
     */
    private String status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 
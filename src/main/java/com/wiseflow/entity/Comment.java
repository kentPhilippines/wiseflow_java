package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_comment")
public class Comment {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 文章ID
     */
    private Integer newsId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 评论者名称
     */
    private String commenterName;
    
    /**
     * 评论时间
     */
    private LocalDateTime commentTime;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 关联的SEO关键词ID
     */
    private Integer seoKeywordId;
    
    /**
     * 是否是AI生成
     */
    private Boolean isAiGenerated;
    
    /**
     * 所属域名
     */
    private String domainConfig;
} 
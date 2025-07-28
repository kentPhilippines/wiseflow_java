package com.wiseflow.vo;

import lombok.Data;

/**
 * 评论规则数据传输对象
 */
@Data
public class CommentRuleVO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 每篇文章最少评论数
     */
    private Integer minCommentsPerArticle;

    /**
     * 每篇文章最多评论数
     */
    private Integer maxCommentsPerArticle;

    /**
     * 评论时间范围（小时）
     */
    private Integer commentTimeRange;

    /**
     * 是否启用AI评论
     */
    private Boolean enableAiComment;

    /**
     * 评论中必须包含关键词的比例（0-100）
     */
    private Integer keywordIncludeRate;

    /**
     * 评论最短长度
     */
    private Integer minCommentLength;

    /**
     * 评论最长长度
     */
    private Integer maxCommentLength;

    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 评论模板
     */
    private String commentTemplate;
    
    /**
     * 单个域名ID
     */
    private Integer domainId;
    
    /**
     * 批量域名ID，逗号分隔
     */
    private String domainIds;
    
    /**
     * 联系电话/批次号
     */
    private String phone;
    
    /**
     * 是否覆盖已存在的评论配置
     */
    private boolean overwriteExisting;
} 
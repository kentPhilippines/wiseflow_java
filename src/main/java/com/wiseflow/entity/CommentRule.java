package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_comment_rule")
public class CommentRule {
    
    @TableId(type = IdType.AUTO)
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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;






    /**
     * 是否已插入
     */
    @TableField(exist = false)
    private Boolean isInserted;
} 
package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SEO关键词实体类
 */
@Data
@TableName("wf_seo_keyword")
public class SeoKeyword {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
 
    
    /**
     * 关键词类型（1:主关键词 2:长尾关键词）
     */
    private Integer type;
    
    /**
     * 使用场景（1:文章内容 2:评论 3:两者都用）
     */
    private Integer useScene;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 每篇文章最大插入次数
     */
    private Integer maxInsertions;
    
    /**
     * 是否允许插入标题
     */
    private Boolean allowTitle;
    
    /**
     * 评论情感倾向（1:正面 2:中性 3:负面）
     */
    private Integer commentSentiment;
    
    /**
     * 评论关键词最大重复次数
     */
    private Integer maxCommentRepeat;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 目标URL
     */
    private String targetUrl;
    
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



    /**
     * 关键词
     */
    @TableField(exist = false)
    private String keyword;

    
} 
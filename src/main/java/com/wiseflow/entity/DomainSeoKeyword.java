package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_domain_seo_keyword")
public class DomainSeoKeyword {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 域名配置ID
     */
    private Integer domainConfigId;
    
    /**
     * SEO关键词ID
     */
    private Integer seoKeywordId;
    
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
} 
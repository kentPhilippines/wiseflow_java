package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 域名文章类型配置实体类
 */
@Data
@TableName("wf_domain_article_config")
public class DomainArticleConfig {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 域名ID
     */
    private Long domainId;
    
    /**
     * 文章类型ID
     */
    private Long typeId;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 
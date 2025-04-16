package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章分配规则实体类
 */
@Data
@TableName("wf_article_rule")
public class ArticleRule {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 规则名称
     */
    private String name;
    
    /**
     * 配置规则JSON字符串
     */
    private String ruleConfig;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 是否启用：0-禁用，1-启用
     */
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;




    /**
     * 是否已插入
     */
    @TableField(exist = false)
    private Boolean isInserted;
} 
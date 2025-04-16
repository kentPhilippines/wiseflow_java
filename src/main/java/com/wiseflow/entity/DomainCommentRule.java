package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wf_domain_comment_rule")
public class DomainCommentRule {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 域名配置ID
     */
    private Integer domainConfigId;
    
    /**
     * 评论规则ID
     */
    private Integer commentRuleId;
    
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
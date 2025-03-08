package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_failed_url")
public class FailedUrl {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private String url;
    
    @TableField("spider_name")
    private String spiderName;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField("retry_count")
    private Integer retryCount = 0;
    
    private Integer status = 0; // 0-未处理，1-已处理
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 增加重试次数的便捷方法
    public void incrementRetryCount() {
        this.retryCount = this.retryCount + 1;
    }
    
    // 标记为已处理的便捷方法
    public void markAsProcessed() {
        this.status = 1;
    }
} 
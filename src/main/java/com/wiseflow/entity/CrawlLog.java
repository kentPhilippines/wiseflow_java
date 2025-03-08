package com.wiseflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wf_crawl_log")
public class CrawlLog {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    @TableField("spider_name")
    private String spiderName;
    
    @TableField("start_time")
    private LocalDateTime startTime;
    
    @TableField("end_time")
    private LocalDateTime endTime;
    
    private Float duration;
    
    private Integer status = 0; // 0-进行中，1-成功，2-失败
    
    @TableField("url_count")
    private Integer urlCount = 0;
    
    @TableField("success_count")
    private Integer successCount = 0;
    
    @TableField("fail_count")
    private Integer failCount = 0;
    
    @TableField("error_message")
    private String errorMessage;
    
    // 开始爬取的便捷方法
    public static CrawlLog start(String spiderName) {
        CrawlLog log = new CrawlLog();
        log.setSpiderName(spiderName);
        log.setStartTime(LocalDateTime.now());
        log.setStatus(0); // 进行中
        return log;
    }
    
    // 完成爬取的便捷方法
    public void complete(boolean success) {
        this.setEndTime(LocalDateTime.now());
        this.setStatus(success ? 1 : 2); // 1-成功，2-失败
        if (this.getStartTime() != null && this.getEndTime() != null) {
            long seconds = java.time.Duration.between(this.getStartTime(), this.getEndTime()).getSeconds();
            this.setDuration((float) seconds);
        }
    }
    
    // 增加URL计数的便捷方法
    public void incrementUrlCount() {
        this.urlCount = this.urlCount + 1;
    }
    
    // 增加成功计数的便捷方法
    public void incrementSuccessCount() {
        this.successCount = this.successCount + 1;
    }
    
    // 增加失败计数的便捷方法
    public void incrementFailCount() {
        this.failCount = this.failCount + 1;
    }
} 
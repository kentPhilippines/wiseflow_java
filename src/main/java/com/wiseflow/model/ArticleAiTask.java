package com.wiseflow.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleAiTask {
    private String taskId;
    private Long articleId;
    private String title;
    private String content;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String result;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
} 
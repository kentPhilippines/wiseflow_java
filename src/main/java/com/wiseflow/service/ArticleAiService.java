package com.wiseflow.service;

import com.wiseflow.model.ArticleAiTask;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.SeoKeyword;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ArticleAiService {
    /**
     * 异步提交文章AI处理任务
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @return 任务ID
     */
    String submitAiTask(Long articleId, String title, String content);

    /**
     * 获取AI处理任务状态
     * @param taskId 任务ID
     * @return 任务状态对象
     */
    ArticleAiTask getTaskStatus(String taskId);

    /**
     * 异步处理文章内容
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @return CompletableFuture<ArticleRewrite> 处理后的文章
     */
    CompletableFuture<ArticleRewrite> processArticleAsync(Long articleId, String title, String content);

    /**
     * 处理待处理的文章
     */
    void processPendingArticles();

    /**
     * 提交一个文章改写任务
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @return 任务ID
     */
    String submitTask(Long articleId, String title, String content);

    /**
     * 获取任务结果
     * @param taskId 任务ID
     * @return 任务结果
     */
    String getTaskResult(String taskId);

    /**
     * 根据关键词规则改写文章
     * @param articleId 文章ID
     * @param title 文章标题
     * @param content 文章内容
     * @param keywords 需要包含的关键词列表
     * @return 改写后的文章
     */
    CompletableFuture<ArticleRewrite> processArticleWithKeywords(Long articleId, String title, String content, List<SeoKeyword> keywords);
} 
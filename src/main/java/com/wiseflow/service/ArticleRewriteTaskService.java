package com.wiseflow.service;

import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.News;
import java.util.List;

public interface ArticleRewriteTaskService {
    /**
     * 添加新闻到改写队列
     * @param news 新闻对象
     * @return 任务ID
     */
    String addNewsToQueue(News news);

    /**
     * 批量添加新闻到改写队列
     * @param newsList 新闻列表
     * @return 任务ID列表
     */
    List<String> addNewsListToQueue(List<News> newsList);

    /**
     * 获取待处理的任务
     * @param limit 限制数量
     * @return 待处理的任务列表
     */
    List<ArticleRewrite> getPendingTasks(int limit);

    /**
     * 获取任务状态
     * @param taskId 任务ID
     * @return 任务状态
     */
    String getTaskStatus(String taskId);

    /**
     * 获取新闻的改写历史
     * @param newsId 新闻ID
     * @return 改写历史列表
     */
    List<ArticleRewrite> getNewsRewriteHistory(Integer newsId);
} 
package com.wiseflow.service;

/**
 * 体育评论服务接口
 */
public interface SportCommentService {
    
    /**
     * 生成体育新闻评论并保存到数据库
     * 
     * @param newsId 新闻ID
     * @param domainConfig 域名配置
     * @return 生成的评论数量
     */
    int generateSportCommentsForNews(Integer newsId, String domainConfig);
    
    /**
     * 为特定体育类型的新闻生成单条评论
     * 
     * @param newsId 新闻ID
     * @param sportType 体育类型
     * @return 生成的评论内容
     */
    String generateSingleSportComment(Integer newsId, String sportType);
} 
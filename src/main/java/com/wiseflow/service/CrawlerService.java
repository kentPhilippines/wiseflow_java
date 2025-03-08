package com.wiseflow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.CrawlLog;
import com.wiseflow.entity.FailedUrl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 爬虫服务接口
 * 使用MyBatis-Plus实现，不使用事务管理
 */
public interface CrawlerService {
    
    /**
     * 启动爬虫
     */
    void startCrawler(String spiderName);
    
    /**
     * 停止爬虫
     */
    void stopCrawler(String spiderName);
    
    /**
     * 重新爬取失败的URL
     */
    void retryCrawlFailedUrls(String spiderName);
    
    /**
     * 保存爬虫日志
     */
    CrawlLog saveCrawlLog(CrawlLog crawlLog);
    
    /**
     * 查询爬虫日志
     */
    CrawlLog findLogById(Integer id);
    
    /**
     * 分页查询爬虫日志
     */
    Page<CrawlLog> findAllLogs(Page<CrawlLog> pageable);
    
    /**
     * 查询指定爬虫的日志
     */
    List<CrawlLog> findLogsBySpiderName(String spiderName);
    
    /**
     * 查询指定时间范围内的日志
     */
    List<CrawlLog> findLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询正在进行中的爬虫任务
     */
    List<CrawlLog> findRunningCrawlers();
    
    /**
     * 获取爬虫统计信息
     */
    Map<String, Object> getCrawlerStats();
    
    /**
     * 保存失败的URL
     */
    FailedUrl saveFailedUrl(FailedUrl failedUrl);
    
    /**
     * 查询失败的URL
     */
    FailedUrl findFailedUrlById(Integer id);
    
    /**
     * 分页查询失败的URL
     */
    Page<FailedUrl> findAllFailedUrls(Page<FailedUrl> pageable);
    
    /**
     * 查询指定爬虫的失败URL
     */
    List<FailedUrl> findFailedUrlsBySpiderName(String spiderName);
    
    /**
     * 查询未处理的失败URL
     */
    List<FailedUrl> findUnprocessedFailedUrls();
    
    /**
     * 标记失败URL为已处理
     */
    void markFailedUrlAsProcessed(Integer id);
    
    /**
     * 删除失败URL
     */
    void deleteFailedUrl(Integer id);
} 
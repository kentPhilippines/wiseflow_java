package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.CrawlLog;
import com.wiseflow.entity.FailedUrl;
import com.wiseflow.mapper.CrawlLogMapper;
import com.wiseflow.mapper.FailedUrlMapper;
import com.wiseflow.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 爬虫服务实现类
 * 使用MyBatis-Plus实现，不使用事务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {
    
    private final CrawlLogMapper crawlLogMapper;
    private final FailedUrlMapper failedUrlMapper;
    
    // 存储正在运行的爬虫进程
    private final Map<String, Process> runningCrawlers = new ConcurrentHashMap<>();
    
    @Override
    public void startCrawler(String spiderName) {
        if (runningCrawlers.containsKey(spiderName)) {
            log.warn("爬虫 {} 已经在运行中", spiderName);
            return;
        }
        
        try {
            // 创建爬虫日志
            CrawlLog crawlLog = new CrawlLog();
            crawlLog.setSpiderName(spiderName);
            crawlLog.setStartTime(LocalDateTime.now());
            crawlLog.setStatus(0); // 0-进行中
            saveCrawlLog(crawlLog);
            
            // 启动爬虫进程
            ProcessBuilder processBuilder = new ProcessBuilder("python", "crawlers/" + spiderName + ".py");
            Process process = processBuilder.start();
            runningCrawlers.put(spiderName, process);
            
            // 异步监控爬虫进程
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    
                    // 更新爬虫日志
                    crawlLog.setEndTime(LocalDateTime.now());
                    crawlLog.setStatus(exitCode == 0 ? 1 : 2); // 1-成功，2-失败
                    saveCrawlLog(crawlLog);
                    
                    // 移除运行中的爬虫
                    runningCrawlers.remove(spiderName);
                    
                    log.info("爬虫 {} 已完成，退出码: {}", spiderName, exitCode);
                } catch (InterruptedException e) {
                    log.error("监控爬虫进程异常", e);
                }
            }).start();
            
            log.info("爬虫 {} 已启动", spiderName);
        } catch (Exception e) {
            log.error("启动爬虫失败", e);
            throw new RuntimeException("启动爬虫失败: " + e.getMessage());
        }
    }
    
    @Override
    public void stopCrawler(String spiderName) {
        Process process = runningCrawlers.get(spiderName);
        if (process != null) {
            process.destroy();
            runningCrawlers.remove(spiderName);
            log.info("爬虫 {} 已停止", spiderName);
        } else {
            log.warn("爬虫 {} 未在运行", spiderName);
        }
    }
    
    @Override
    public void retryCrawlFailedUrls(String spiderName) {
        List<FailedUrl> failedUrls = findFailedUrlsBySpiderName(spiderName);
        if (failedUrls.isEmpty()) {
            log.info("没有需要重试的URL");
            return;
        }
        
        log.info("开始重试爬取 {} 个失败的URL", failedUrls.size());
        
        // TODO: 实现重试逻辑
        
        log.info("重试爬取完成");
    }
    
    @Override
    public CrawlLog saveCrawlLog(CrawlLog crawlLog) {
        if (crawlLog.getId() == null) {
            crawlLogMapper.insert(crawlLog);
        } else {
            crawlLogMapper.updateById(crawlLog);
        }
        return crawlLog;
    }
    
    @Override
    public CrawlLog findLogById(Integer id) {
        CrawlLog crawlLog = crawlLogMapper.selectById(id);
        if (crawlLog == null) {
            throw new NoSuchElementException("CrawlLog not found with id: " + id);
        }
        return crawlLog;
    }
    
    @Override
    public Page<CrawlLog> findAllLogs(Page<CrawlLog> pageable) {
        return crawlLogMapper.selectPage(pageable, null);
    }
    
    @Override
    public List<CrawlLog> findLogsBySpiderName(String spiderName) {
        return crawlLogMapper.selectBySpiderName(spiderName);
    }
    
    @Override
    public List<CrawlLog> findLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return crawlLogMapper.selectByTimeRange(startTime, endTime);
    }
    
    @Override
    public List<CrawlLog> findRunningCrawlers() {
        return crawlLogMapper.selectByStatus(0); // 0-进行中
    }
    
    @Override
    public Map<String, Object> getCrawlerStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取爬虫成功率统计
        List<Object[]> successRateStats = crawlLogMapper.getSpiderSuccessRateStats();
        stats.put("successRateStats", successRateStats);
        
        // 获取正在运行的爬虫数量
        long runningCount = crawlLogMapper.selectByStatus(0).size();
        stats.put("runningCount", runningCount);
        
        // 获取失败URL数量
        long failedUrlCount = failedUrlMapper.selectByStatus(0).size();
        stats.put("failedUrlCount", failedUrlCount);
        
        return stats;
    }
    
    @Override
    public FailedUrl saveFailedUrl(FailedUrl failedUrl) {
        if (failedUrl.getId() == null) {
            failedUrlMapper.insert(failedUrl);
        } else {
            failedUrlMapper.updateById(failedUrl);
        }
        return failedUrl;
    }
    
    @Override
    public FailedUrl findFailedUrlById(Integer id) {
        FailedUrl failedUrl = failedUrlMapper.selectById(id);
        if (failedUrl == null) {
            throw new NoSuchElementException("FailedUrl not found with id: " + id);
        }
        return failedUrl;
    }
    
    @Override
    public Page<FailedUrl> findAllFailedUrls(Page<FailedUrl> pageable) {
        return failedUrlMapper.selectPage(pageable, null);
    }
    
    @Override
    public List<FailedUrl> findFailedUrlsBySpiderName(String spiderName) {
        return failedUrlMapper.selectBySpiderName(spiderName);
    }
    
    @Override
    public List<FailedUrl> findUnprocessedFailedUrls() {
        return failedUrlMapper.selectByStatus(0); // 0-未处理
    }
    
    @Override
    public void markFailedUrlAsProcessed(Integer id) {
        FailedUrl failedUrl = findFailedUrlById(id);
        failedUrl.markAsProcessed();
        failedUrlMapper.updateById(failedUrl);
    }
    
    @Override
    public void deleteFailedUrl(Integer id) {
        failedUrlMapper.deleteById(id);
    }
} 
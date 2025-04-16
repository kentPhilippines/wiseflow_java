package com.wiseflow.service.impl;

import com.wiseflow.service.ArticleRewriteTaskService;
import com.wiseflow.service.ArticleAiService;
import com.wiseflow.entity.ArticleRewrite;
import com.wiseflow.entity.Article;
import com.wiseflow.entity.News;
import com.wiseflow.entity.NewsContent;
import com.wiseflow.entity.NewsImage;
import com.wiseflow.mapper.ArticleRewriteMapper;
import com.wiseflow.mapper.ArticleMapper;
import com.wiseflow.mapper.NewsMapper;
import com.wiseflow.mapper.NewsContentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleRewriteTaskServiceImpl implements ArticleRewriteTaskService {

    private final ArticleAiService articleAiService;
    private final ArticleRewriteMapper articleRewriteMapper;
    private final ArticleMapper articleMapper;
    private final NewsMapper newsMapper;
    private final NewsContentMapper newsContentMapper;
    private final Map<String, String> taskStatusMap = new ConcurrentHashMap<>();
    
    public ArticleRewriteTaskServiceImpl(ArticleAiService articleAiService, 
                                       ArticleRewriteMapper articleRewriteMapper,
                                       ArticleMapper articleMapper,
                                       NewsMapper newsMapper,
                                       NewsContentMapper newsContentMapper) {
        this.articleAiService = articleAiService;
        this.articleRewriteMapper = articleRewriteMapper;
        this.articleMapper = articleMapper;
        this.newsMapper = newsMapper;
        this.newsContentMapper = newsContentMapper;
    }

    @Override
    public String addNewsToQueue(News news) {
        try {
            // 获取新闻内容
            NewsContent newsContent = newsContentMapper.selectById(news.getId());
            if (newsContent == null) {
                log.warn("新闻内容不存在，跳过改写: newsId={}", news.getId());
                // 更新新闻状态为失败
                news.setNeedRewrite(true);
                news.setRewriteStatus("FAILED");
                newsMapper.updateById(news);
                return null;
            }
            
            String taskId = UUID.randomUUID().toString();
            
            // 更新新闻状态为待改写
            news.setNeedRewrite(true);
            news.setRewriteStatus("PENDING");
            newsMapper.updateById(news);
            
            taskStatusMap.put(taskId, "PENDING");
            return taskId;
        } catch (Exception e) {
            log.error("添加新闻到改写队列失败: newsId={}, error={}", news.getId(), e.getMessage(), e);
            // 更新新闻状态为失败
            news.setNeedRewrite(true);
            news.setRewriteStatus("FAILED");
            newsMapper.updateById(news);
            return null;
        }
    }

    @Override
    public List<String> addNewsListToQueue(List<News> newsList) {
        // 改为串行处理，一次只处理一条新闻
        List<String> taskIds = new ArrayList<>();
        for (News news : newsList) {
            try {
                String taskId = addNewsToQueue(news);
                taskIds.add(taskId);
            } catch (Exception e) {
                log.error("添加新闻到队列失败: {}", news.getId(), e);
            }
        }
        return taskIds;
    }

    @Override
    public List<ArticleRewrite> getPendingTasks(int limit) {
        // 查询原新闻表中需要改写的文章
        LambdaQueryWrapper<News> newsQuery = new LambdaQueryWrapper<>();
        newsQuery.eq(News::getNeedRewrite, true)
                .eq(News::getRewriteStatus, "PENDING")
                .orderByAsc(News::getPublishTime)
                .last("LIMIT " + limit);
        
        List<News> pendingNews = newsMapper.selectList(newsQuery);
        List<ArticleRewrite> tasks = new ArrayList<>();
        
        for (News news : pendingNews) {
            NewsContent content = newsContentMapper.selectById(news.getId());
            if (content != null) {
                ArticleRewrite task = new ArticleRewrite();
                task.setOriginalArticleId(news.getId().longValue());
                task.setTitle(news.getTitle());
                task.setContent(content.getContent());
                task.setStatus("PENDING");
                tasks.add(task);
            }
        }
        
        return tasks;
    }

    @Override
    public String getTaskStatus(String taskId) {
        return taskStatusMap.getOrDefault(taskId, "NOT_FOUND");
    }

    @Override
    public List<ArticleRewrite> getNewsRewriteHistory(Integer newsId) {
        LambdaQueryWrapper<ArticleRewrite> query = new LambdaQueryWrapper<>();
        query.eq(ArticleRewrite::getOriginalArticleId, newsId)
             .orderByDesc(ArticleRewrite::getCreateTime);
        return articleRewriteMapper.selectList(query);
    }

  //  @Scheduled(fixedRate = 10000) // 每1分钟执行一次
    @Transactional
    public void processArticles() {
        log.info("开始处理待改写文章...");
        
        ArticleRewrite task = getNextPendingTask();
        if (task == null) {
            log.info("没有待处理的文章");
            return;
        }

        try {
            log.info("开始处理新闻ID: {}", task.getOriginalArticleId());
            
            // 提交到AI服务处理
            String taskId = articleAiService.submitAiTask(
                task.getOriginalArticleId(),
                task.getTitle(),
                task.getContent()
            );

            log.info("新闻已提交处理, ID: {}, TaskID: {}", task.getOriginalArticleId(), taskId);
        } catch (Exception e) {
            log.error("处理新闻失败, ID: " + task.getOriginalArticleId(), e);
            // 更新原新闻状态
            News news = new News();
            news.setId(task.getOriginalArticleId().intValue());
            news.setRewriteStatus("FAILED");
            newsMapper.updateById(news);
        }
    }

    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    @Transactional
    public void handleCompletedTask() {
        log.info("开始处理已完成的改写任务...");
        
        // 查询一条已完成的任务
        LambdaQueryWrapper<ArticleRewrite> query = new LambdaQueryWrapper<>();
        query.eq(ArticleRewrite::getStatus, "COMPLETED")
             .orderByAsc(ArticleRewrite::getCreateTime)
             .last("LIMIT 1");
        
        ArticleRewrite task = articleRewriteMapper.selectOne(query);
        if (task == null) {
            return;
        }
        
        try {
            // 获取原新闻
            News originalNews = newsMapper.selectById(task.getOriginalArticleId().intValue());
            if (originalNews == null) {
                log.error("原新闻不存在: {}", task.getOriginalArticleId());
                return;
            }
            
            NewsContent originalContent = newsContentMapper.selectById(originalNews.getId());
            if (originalContent == null) {
                log.error("原新闻内容不存在: {}", originalNews.getId());
                return;
            }
            
            // 创建新的文章记录
            Article newArticle = new Article();
            newArticle.setTitle(task.getTitle());
            newArticle.setContent(task.getContent());
            newArticle.setCategoryId(originalNews.getCategoryId());
            newArticle.setCategoryName(originalNews.getCategoryName());
            newArticle.setSource("(AI改写)");
            newArticle.setAuthor(originalNews.getAuthor());
            newArticle.setUrl(originalNews.getUrl());
            newArticle.setPublishTime(LocalDateTime.now());
            newArticle.setCreateTime(LocalDateTime.now());
            newArticle.setUpdateTime(LocalDateTime.now());
            newArticle.setOriginalArticleId(task.getOriginalArticleId()); // 关联原新闻ID
            articleMapper.insert(newArticle);
            
            // 更新原新闻状态
            originalNews.setRewriteStatus("COMPLETED");
            newsMapper.updateById(originalNews);
            
            // 更新任务状态
            task.setStatus("ARCHIVED");
            articleRewriteMapper.updateById(task);
            
            log.info("文章改写完成并创建新记录, 原新闻ID: {}, 新文章ID: {}", task.getOriginalArticleId(), newArticle.getId());
        } catch (Exception e) {
            log.error("处理已完成任务失败, ID: " + task.getOriginalArticleId(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            articleRewriteMapper.updateById(task);
            
            // 更新原新闻状态为失败
            News news = new News();
            news.setId(task.getOriginalArticleId().intValue());
            news.setRewriteStatus("FAILED");
            newsMapper.updateById(news);
        }
    }

    private ArticleRewrite getNextPendingTask() {
        // 查询一条待处理的新闻
        LambdaQueryWrapper<News> newsQuery = new LambdaQueryWrapper<>();
        newsQuery.eq(News::getNeedRewrite, true)
                .eq(News::getRewriteStatus, "PENDING")
                .orderByDesc(News::getPublishTime)
                .last("LIMIT 1");
        
        News pendingNews = newsMapper.selectOne(newsQuery);
        if (pendingNews == null) {
            return null;
        }

        NewsContent content = newsContentMapper.selectById(pendingNews.getId());
        if (content == null) {
            log.error("新闻内容不存在: {}", pendingNews.getId());
            // 更新新闻状态为失败
            pendingNews.setRewriteStatus("FAILED");
            newsMapper.updateById(pendingNews);
            return null;
        }

        // 更新新闻状态为处理中
        pendingNews.setRewriteStatus("PROCESSING");
        newsMapper.updateById(pendingNews);

        ArticleRewrite task = new ArticleRewrite();
        task.setOriginalArticleId(pendingNews.getId().longValue());
        task.setTitle(pendingNews.getTitle());
        task.setContent(content.getContent());
        task.setStatus("PENDING");
        return task;
    }
} 
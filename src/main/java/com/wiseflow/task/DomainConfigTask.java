package com.wiseflow.task;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wiseflow.entity.*;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.service.NewsService;
import com.wiseflow.service.CommentService;
import com.wiseflow.service.ArticleService;
import com.wiseflow.util.ArticleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Resource;

/**
 * 域名配置定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainConfigTask {

    private final DomainConfigService domainConfigService;
    private final NewsService newsService;
    private final CommentService commentService;
    private final ArticleService articleService;
    private final ArticleUtil articleUtil;
    
    // 使用原子布尔值作为任务锁
    private final AtomicBoolean taskLock = new AtomicBoolean(false);
    
    // 缓存上次同步的时间
    private final Map<String, LocalDateTime> lastSyncTimeMap = new ConcurrentHashMap<>();
    
    // 批处理大小
    private static final int BATCH_SIZE = 100;

    // 原子任务状态，避免任务重叠执行
    private final AtomicBoolean articleRewriteTaskRunning = new AtomicBoolean(false);

    /**
     * 每5分钟执行一次配置同步
     */
    @Scheduled(fixedRate = 300 )
    public void syncDomainConfigs() {
        // 如果上一次任务还在执行,则跳过本次执行
        if (!taskLock.compareAndSet(false, true)) {
            log.info("上一次同步任务还在执行,跳过本次同步");
            return;
        }

        try {
            List<DomainConfig> domainConfigs = domainConfigService.findAllEnabled();
            log.info("开始同步域名配置,共{}个域名...", domainConfigs.size());

            // 异步执行各个域名的同步任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // 同步文章配置
            futures.add(CompletableFuture.runAsync(() -> syncArticleConfigs(domainConfigs)));
            
            // 同步SEO关键词配置
            futures.add(CompletableFuture.runAsync(() -> syncSeoKeywords(domainConfigs)));
            
            // 同步评论规则配置
            futures.add(CompletableFuture.runAsync(() -> syncCommentRules(domainConfigs)));

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 异步触发文章改写任务
            asyncRewriteArticlesForAllDomains();
            
            log.info("域名配置同步完成");
        } catch (Exception e) {
            log.error("域名配置同步失败", e);
        } finally {
            // 释放任务锁
            taskLock.set(false);
        }
    }

    /**
     * 同步SEO关键词配置
     * 这里主要用于 改写文章
     */
    @Async
    protected void syncSeoKeywords(List<DomainConfig> domainConfigs) {
        domainConfigs.parallelStream().forEach(domainConfig -> {
            String domain = domainConfig.getDomain();
            log.info("同步SEO关键词配置...{}", domain);
            List<SeoKeyword> keywords = domainConfigService.getAllDomainKeywords(domainConfig);
            List<String> keywordsSeo = domainConfig.getKeywordsSeo();
            
            // 添加文章改写调用
            if (!keywords.isEmpty()) {
                rewriteArticlesByDomainConfig(domainConfig, keywords);
            } else {
                log.info("域名{}没有配置关键词，不执行文章改写", domain);
            }
        });
    }

    /**
     * 同步评论规则配置
     */
    @Async
    protected void syncCommentRules(List<DomainConfig> domainConfigs) {
        domainConfigs.parallelStream().forEach(domainConfig -> {
            String domain = domainConfig.getDomain();
            log.info("同步评论规则配置...{}", domain);
            List<CommentRule> rules = domainConfigService.getAllDomainCommentRules(domainConfig);

            try {
                // 1. 处理无评论的文章(优先处理)
                processUncommentedArticles(domain, rules);
                
                // 2. 随机处理历史文章(增加自然度)
                processHistoricalArticles(domain, rules);
                
            } catch (Exception e) {
                log.error("处理域名[{}]评论规则失败", domain, e);
            }
        });
    }
    
    /**
     * 处理无评论的文章
     */
    private void processUncommentedArticles(String domain, List<CommentRule> rules) {
        // 获取所有无评论的文章
        List<News> uncommentedArticles = newsService.getUncommentedArticles(domain);
        
        if (uncommentedArticles.isEmpty()) {
            log.debug("域名[{}]没有未评论的文章", domain);
            return;
        }
        
        // 随机打乱文章顺序
        java.util.Collections.shuffle(uncommentedArticles);
        
        // 处理每篇无评论的文章
        uncommentedArticles.parallelStream().forEach(article -> {
            try {
                processArticleComments(article, rules);
            } catch (Exception e) {
                log.error("处理无评论文章失败: domain={}, articleId={}", domain, article.getId(), e);
            }
        });
    }
    
    /**
     * 处理历史文章的随机评论
     */
    private void processHistoricalArticles(String domain, List<CommentRule> rules) {
        // 1. 获取最早的文章时间
        LocalDateTime earliestTime = newsService.getEarliestNewsTime();
        if (earliestTime == null) {
            return;
        }
        
        // 2. 计算时间范围(从最早文章到现在)
        LocalDateTime now = LocalDateTime.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(earliestTime.toLocalDate(), now.toLocalDate());
        
        // 3. 随机选择时间段
        int randomSegments = (int) (Math.random() * 5) + 1; // 随机选择1-5个时间段
        for (int i = 0; i < randomSegments; i++) {
            // 随机选择一个时间点
            long randomDays = (long) (Math.random() * daysBetween);
            LocalDateTime randomDate = earliestTime.plusDays(randomDays);
            
            // 随机选择时间窗口(1天到30天)
            int windowDays = (int) (Math.random() * 29) + 1;
            LocalDateTime windowEnd = randomDate.plusDays(windowDays);
            
            // 获取该时间段的文章
            List<News> articles = newsService.getArticlesByTimeRange(
                domain,
                randomDate,
                windowEnd,
                100 // 限制数量
            );
            
            if (articles.isEmpty()) {
                continue;
            }
            
            // 随机选择部分文章进行评论
            java.util.Collections.shuffle(articles);
            int articlesToProcess = Math.min(
                10, // 每个时间段最多处理10篇
                (int) (articles.size() * (0.1 + Math.random() * 0.2)) // 随机选择10%-30%的文章
            );
            
            // 处理选中的文章
            articles.subList(0, articlesToProcess).parallelStream().forEach(article -> {
                try {
                    // 检查当前评论数
                    int currentComments = commentService.countByNewsId(article.getId());
                    
                    // 只处理评论数较少的文章
                    if (shouldAddRandomComment(currentComments)) {
                        // 随机选择要添加的评论数(1-3条)
                        int commentsToAdd = (int) (Math.random() * 3) + 1;
                        
                        // 随机选择一个评论规则
                        CommentRule randomRule = rules.get((int) (Math.random() * rules.size()));
                        List<CommentRule> singleRule = List.of(randomRule);
                        
                        // 生成评论
                        commentService.generateAiComments(article.getId(), commentsToAdd, singleRule);
                        
                        // 添加随机延迟(0-10秒)
                        Thread.sleep((long) (Math.random() * 10000));
                    }
                } catch (Exception e) {
                    log.error("处理历史文章评论失败: domain={}, articleId={}", domain, article.getId(), e);
                }
            });
        }
    }
    
    /**
     * 判断是否需要添加随机评论
     * 评论数越少,越有可能被选中添加评论
     */
    private boolean shouldAddRandomComment(int currentComments) {
        // 根据当前评论数决定添加评论的概率
        double probability;
        if (currentComments == 0) {
            probability = 0.8; // 无评论文章80%概率添加
        } else if (currentComments <= 3) {
            probability = 0.4; // 1-3条评论40%概率添加
        } else if (currentComments <= 5) {
            probability = 0.2; // 4-5条评论20%概率添加
        } else if (currentComments <= 10) {
            probability = 0.1; // 6-10条评论10%概率添加
        } else {
            probability = 0.05; // 10条以上评论5%概率添加
        }
        
        return Math.random() < probability;
    }

    /**
     * 处理单篇文章的评论
     */
    private void processArticleComments(News article, List<CommentRule> rules) {
        // 获取文章当前评论数
        int currentComments = commentService.countByNewsId(article.getId());
        // 遍历评论规则
        for (CommentRule rule : rules) {
            if (!rule.getEnabled()) {
                continue;
            }
            // 检查是否需要添加评论
            int minComments = rule.getMinCommentsPerArticle();
            int maxComments = rule.getMaxCommentsPerArticle();
            
            if (currentComments >= minComments) {
                log.debug("文章[{}]评论数已满足最小要求", article.getId());
                continue;
            }
            
            // 计算需要添加的评论数
            int needComments = Math.min(
                minComments - currentComments,
                maxComments - currentComments
            );
            
            if (needComments <= 0) {
                continue;
            }
            
            log.info("文章[{}]需要添加{}条评论", article.getId(), needComments);

            List<CommentRule> rules1 = new ArrayList<>();
            rules1.add(rule);
            commentService.generateAiComments(article.getId(), needComments, rules1);
        }
    }
     
    /**
     * 同步文章配置
     * 为每个域名分配文章
     */
    @Async
    protected void syncArticleConfigs(List<DomainConfig> domainConfigs) {
        domainConfigs.parallelStream().forEach(domainConfig -> {
            String domain = domainConfig.getDomain();
            log.info("同步文章配置...{}", domain);
            // 获取上次同步时间,如果没有则取文章库最早的文章时间
            LocalDateTime lastSyncTime = lastSyncTimeMap.getOrDefault(domain, newsService.getEarliestNewsTime());
            // 获取域名的文章配置规则
            List<ArticleRule> configs = domainConfigService.getAllDomainArticleConfigs(domainConfig);
            
            configs.forEach(config -> {
                try {
                    processArticleRule(domainConfig, config, lastSyncTime);
                } catch (Exception e) {
                    log.error("处理文章规则配置失败: domain={}, config={}", domain, config, e);
                }
            });
            
            // 更新同步时间
            lastSyncTimeMap.put(domain, LocalDateTime.now());
        });
    }
    
    /**
     * 处理单个文章规则配置
     */
    private void processArticleRule(DomainConfig domainConfig, ArticleRule config, LocalDateTime lastSyncTime) {
        log.info("处理文章规则配置: {}", config);
        Object ruleConfig = config.getRuleConfig();
        JSONArray rules = JSONUtil.parseArray(ruleConfig);
        
        // 获取需要处理的时间范围
        LocalDateTime startTime = lastSyncTime;
        LocalDateTime endTime = LocalDateTime.now();
        
        // 按天遍历时间范围
        LocalDate currentDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = currentDate.atStartOfDay();
            LocalDateTime dayEnd = currentDate.plusDays(1).atStartOfDay();
            
            // 检查当前时间段是否有文章
            Long unassignedCount = newsService.countUnassignedNews(domainConfig.getDomain(), dayStart, dayEnd);
            if (unassignedCount == 0) {
                log.debug("当前时间段[{} - {}]没有待分配的文章,跳过处理", dayStart, dayEnd);
                currentDate = currentDate.plusDays(1);
                continue;
            }
            
            // 并行处理每个分类的规则
            List<CompletableFuture<Void>> futures = rules.stream()
                .map(rule -> {
                    JSONObject ruleObj = (JSONObject) rule;
                    return CompletableFuture.runAsync(() -> 
                        processArticleTypeRule(domainConfig, dayStart, dayEnd, ruleObj)
                    );
                })
                .collect(Collectors.toList());
                
            // 等待所有规则处理完成    
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            currentDate = currentDate.plusDays(1);
        }
    }
    
    /**
     * 处理单个文章分类的规则
     */
    private void processArticleTypeRule(DomainConfig domainConfig, LocalDateTime dayStart, LocalDateTime dayEnd, JSONObject ruleObj) {
        String typeId = ruleObj.getStr("typeId");
        int minCount = ruleObj.getInt("minCount", 10);
        int maxCount = ruleObj.getInt("maxCount", 30);
        
        // 检查当前日期该分类的文章分配情况
        int assignedCount = newsService.countAssignedNews(
            domainConfig.getDomain(), 
            typeId, 
            dayStart, 
            dayEnd
        );
        
        // 如果分配数量不足最小值,进行分配
        if (assignedCount < minCount) {
            int needCount = minCount - assignedCount;
            
            // 分批获取和处理文章
            for (int offset = 0; offset < needCount; offset += BATCH_SIZE) {
                int batchSize = Math.min(BATCH_SIZE, needCount - offset);
                
                // 获取可分配的文章
                List<News> availableNews = newsService.getUnassignedNews(
                    typeId,
                    dayStart,
                    dayEnd,
                    batchSize
                );
                
                if (availableNews.isEmpty()) {
                    break;
                }
                
                // 分配文章
                log.info("为域名[{}]分类[{}]分配{}篇文章", 
                    domainConfig.getDomain(),
                    typeId,
                    availableNews.size()
                );
                
                availableNews.forEach(news -> {
                    try {
                        news.setDomainConfig(domainConfig.getDomain());
                        news.setUpdateTime(LocalDateTime.now());
                        newsService.updateDomain(news);

                    } catch (Exception e) {
                        log.error("更新文章分配信息失败: newsId={}", news.getId(), e);
                    }
                });
            }
        }
    }

    /**
     * 根据域名配置和关键词改写文章
     */
    private void rewriteArticlesByDomainConfig(DomainConfig domainConfig, List<SeoKeyword> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            log.info("域名{}没有可用的SEO关键词，跳过文章改写", domainConfig.getDomain());
            return;
        }
        
        try {
            log.info("开始根据域名{}的关键词配置改写文章", domainConfig.getDomain());
            
            // 1. 查询需要改写的文章列表（假设每次处理5篇文章）
            List<Article> articlesToRewrite = articleService.getArticlesToRewrite(domainConfig.getDomain(), 5);
            if (articlesToRewrite.isEmpty()) {
                log.info("域名{}下没有需要改写的文章", domainConfig.getDomain());
                return;
            }
            
            log.info("找到{}篇待改写文章", articlesToRewrite.size());
            
            // 2. 处理每篇文章
            for (Article article : articlesToRewrite) {
                try {
                    log.info("开始改写文章: articleId={}, title={}", article.getId(), article.getTitle());
                    
                    // 使用ArticleUtil进行改写
                    ArticleRewrite rewrittenArticle = articleUtil.rewriteArticleWithDomainConfig(
                            article.getId(), domainConfig);
                    
                    if (rewrittenArticle != null) {
                        log.info("文章改写成功: articleId={}, newTitle={}, originalityScore={}", 
                                article.getId(), rewrittenArticle.getTitle(), rewrittenArticle.getOriginalityScore());
                        
                        // 更新文章改写状态（这里假设有这个方法）
                        articleService.updateArticleRewriteStatus(article.getId(), true);
                    } else {
                        log.warn("文章改写失败: articleId={}", article.getId());
                    }
                    
                } catch (Exception e) {
                    log.error("改写文章过程中出错: articleId={}, error={}", article.getId(), e.getMessage(), e);
                }
                
                // 为防止API限制，每次改写后短暂休息
                try {
                    Thread.sleep(5000); // 5秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("改写文章线程被中断");
                    break;
                }
            }
            
            log.info("域名{}的文章改写任务完成", domainConfig.getDomain());
            
        } catch (Exception e) {
            log.error("域名{}的文章改写任务异常: {}", domainConfig.getDomain(), e.getMessage(), e);
        }
    }

    /**
     * 异步触发所有域名的文章改写任务
     */
    private void asyncRewriteArticlesForAllDomains() {
        // 使用新线程执行，避免阻塞主任务
        CompletableFuture.runAsync(() -> {
            if (!articleRewriteTaskRunning.compareAndSet(false, true)) {
                log.info("文章改写任务已在运行中，跳过本次执行");
                return;
            }
            
            try {
                log.info("开始执行文章改写任务...");
                
                // 获取所有启用的域名配置
                List<DomainConfig> enabledDomainConfigs = domainConfigService.getEnabledDomainConfigs();
                
                for (DomainConfig domainConfig : enabledDomainConfigs) {
                    try {
                        // 获取域名的关键词配置
                        List<SeoKeyword> keywords = domainConfigService.getAllDomainKeywords(domainConfig);
                        
                        // 执行文章改写
                        rewriteArticlesByDomainConfig(domainConfig, keywords);
                        
                        // 等待一段时间，避免频繁请求
                        Thread.sleep(10000); // 10秒
                    } catch (Exception e) {
                        log.error("处理域名{}的文章改写任务异常: {}", domainConfig.getDomain(), e.getMessage());
                        // 继续处理下一个域名
                    }
                }
                
                log.info("文章改写任务执行完成");
            } catch (Exception e) {
                log.error("执行文章改写任务时发生错误: {}", e.getMessage(), e);
            } finally {
                // 无论任务成功或失败，都释放任务锁
                articleRewriteTaskRunning.set(false);
            }
        });
    }
    
    /**
     * 定时触发文章改写任务
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void scheduledArticleRewriteTask() {
        log.info("定时文章改写任务开始...");
        asyncRewriteArticlesForAllDomains();
    }
} 
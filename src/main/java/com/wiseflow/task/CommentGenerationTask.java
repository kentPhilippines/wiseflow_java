package com.wiseflow.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.CommentRule;
import com.wiseflow.entity.News;
import com.wiseflow.service.CommentRuleService;
import com.wiseflow.service.CommentService;
import com.wiseflow.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 评论自动生成定时任务
 * 根据评论规则自动为文章生成评论
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class CommentGenerationTask {

    private final NewsService newsService;
    private final CommentService commentService;
    private final CommentRuleService commentRuleService;
    
    // 已处理的文章ID缓存，使用ConcurrentHashMap实现线程安全
    private final Set<Integer> processedNewsIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // 线程池配置参数
    @Value("${comment.generation.thread.core-pool-size:5}")
    private int corePoolSize;
    
    @Value("${comment.generation.thread.max-pool-size:10}")
    private int maxPoolSize;
    
    @Value("${comment.generation.thread.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${comment.generation.thread.keep-alive-seconds:60}")
    private int keepAliveSeconds;
    
    // 定义线程池
    private ThreadPoolExecutor commentGenerationExecutor;
    
    // 定义线程工厂，自定义线程名称
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String namePrefix) {
            group =   Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    @PostConstruct
    public void init() {
        // 初始化线程池
        commentGenerationExecutor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveSeconds,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new NamedThreadFactory("comment-gen-"),
            new ThreadPoolExecutor.CallerRunsPolicy()  // 当队列满时，使用调用者所在的线程来运行任务
        );
        log.info("评论生成线程池初始化完成，核心线程数: {}, 最大线程数: {}", corePoolSize, maxPoolSize);
    }
    
    @PreDestroy
    public void shutdown() {
        // 优雅关闭线程池
        if (commentGenerationExecutor != null) {
            commentGenerationExecutor.shutdown();
            try {
                if (!commentGenerationExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    commentGenerationExecutor.shutdownNow();
                    if (!commentGenerationExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                        log.error("评论生成线程池未能正常关闭");
                    }
                }
            } catch (InterruptedException ie) {
                commentGenerationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("评论生成线程池已关闭");
        }
    }
    
    /**
     * 每10分钟执行一次评论生成任务
     */
    @Scheduled(fixedRate = 600000)
    public void generateComments() {
      /*   log.info("开始执行评论自动生成任务...");
        
        try {
            // 获取所有启用的评论规则，按域名分组
            Map<String, List<CommentRule>> rulesByDomain = new HashMap<>();
            List<CommentRule> allEnabledRules = commentRuleService.findAllEnabled();
            
            if (allEnabledRules.isEmpty()) {
                log.info("没有找到启用的评论规则，跳过处理");
                return;
            }
            
            // 按域名分组规则
            for (CommentRule rule : allEnabledRules) {
                if (!rule.getEnableAiComment()) {
                    continue; // 跳过未启用AI评论的规则
                }
                
                String domain = rule.getDomainConfig();
                if (domain == null || domain.trim().isEmpty()) {
                    log.warn("评论规则[ID={}]的域名配置为空，跳过处理", rule.getId());
                    continue;
                }
                
                // 确保域名格式正确
                domain = domain.trim();
                rulesByDomain.computeIfAbsent(domain, k -> new ArrayList<>()).add(rule);
            }
            
            // 记录所有提交的任务，用于后续等待完成
            List<Future<?>> futures = new ArrayList<>();
            
            // 处理每个域名
            for (Map.Entry<String, List<CommentRule>> entry : rulesByDomain.entrySet()) {
                String domain = entry.getKey();
                List<CommentRule> rules = entry.getValue();
                
                // 查询该域名下需要处理的文章（最近7天发布且评论数不足的文章）
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                List<News> newsList = newsService.searchIsComment(sevenDaysAgo, 50, domain);
                
                // 并行处理文章
                for (News news : newsList) {
                    // 提交到线程池执行
                    Future<?> future = commentGenerationExecutor.submit(() -> {
                        try {
                            processNews(news, rules);
                        } catch (Exception e) {
                            log.error("处理文章[ID={}]任务出错", news.getId(), e);
                        }
                    });
                    futures.add(future);
                }
            }
            
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get(5, TimeUnit.MINUTES); // 设置超时时间，防止单个任务阻塞
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("等待评论生成任务完成时出错", e);
                }
            }
            
            log.info("评论自动生成任务执行完成");
        } catch (Exception e) {
            log.error("评论自动生成任务执行出错", e);
        } */
    }
    
    /**
     * 处理单篇文章
     */
    private void processNews(News news, List<CommentRule> rules) {
        try {
            // 检查是否需要生成评论
            if (checkNeedGenerateComments(news, rules)) {
                generateCommentsForNews(news, rules);
            }
            // 标记已评论
            newsService.isComment(news.getId(), true);
        } catch (Exception e) {
            log.error("处理文章[ID={}]过程中出错", news.getId(), e);
        }
    }
    
    /**
     * 检查文章是否需要生成评论
     */
    private boolean checkNeedGenerateComments(News news, List<CommentRule> rules) {
        // 如果已经处理过，跳过
        if (processedNewsIds.contains(news.getId())) {
            return false;
        }
        
        // 获取文章当前评论数
        int currentCommentCount = commentService.countByNewsId(news.getId());
        
        // 根据规则判断是否需要生成评论
        for (CommentRule rule : rules) {
            if (currentCommentCount < rule.getMinCommentsPerArticle()) {
                return true;
            }
        }
        
        // 如果评论数已经满足所有规则的最低要求，不需要生成
        return false;
    }
    
    /**
     * 为文章生成评论
     */
    private void generateCommentsForNews(News news, List<CommentRule> rules) {
        try {
            // 获取文章当前评论数
            int currentCommentCount = commentService.countByNewsId(news.getId());
            
            // 找出需要生成的评论数量（取所有规则中最大的目标数量）
            int targetCommentCount = rules.stream()
                .mapToInt(CommentRule::getMaxCommentsPerArticle)
                .max()
                .orElse(0);
            
            // 计算需要生成的评论数量
            int needToGenerateCount = targetCommentCount - currentCommentCount;
            
            if (needToGenerateCount <= 0) {
                processedNewsIds.add(news.getId());
                return;
            }
            
            log.info("为文章[ID={}]({})生成{}条评论", news.getId(), news.getTitle(), needToGenerateCount);
            
            // 生成评论
         //   commentService.generateAiComments(news.getId(), needToGenerateCount);
            
            // 添加到已处理缓存
            processedNewsIds.add(news.getId());
            
            // 避免频繁处理，防止API限制
            TimeUnit.SECONDS.sleep(2);
        } catch (Exception e) {
            log.error("为文章[ID={}]生成评论时出错", news.getId(), e);
        }
    }
    
    /**
     * 每天午夜清理处理缓存
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void clearProcessedCache() {
        int size = processedNewsIds.size();
        processedNewsIds.clear();
        log.info("已清理评论生成任务处理缓存，共{}条记录", size);
    }
    
    /**
     * 获取当前线程池状态
     */
    public Map<String, Object> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeCount", commentGenerationExecutor.getActiveCount());
        status.put("corePoolSize", commentGenerationExecutor.getCorePoolSize());
        status.put("maximumPoolSize", commentGenerationExecutor.getMaximumPoolSize());
        status.put("largestPoolSize", commentGenerationExecutor.getLargestPoolSize());
        status.put("taskCount", commentGenerationExecutor.getTaskCount());
        status.put("completedTaskCount", commentGenerationExecutor.getCompletedTaskCount());
        status.put("queueSize", commentGenerationExecutor.getQueue().size());
        status.put("queueRemainingCapacity", commentGenerationExecutor.getQueue().remainingCapacity());
        return status;
    }
} 
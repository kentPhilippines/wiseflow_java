package com.wiseflow.task;

import com.wiseflow.entity.DomainConfig;
import com.wiseflow.entity.News;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
//@Component
@RequiredArgsConstructor
public class DomainAssignTask {

    private final NewsService newsService;
    private final DomainConfigService domainConfigService;
    
    // 用于记录每个域名在不同日期已分配的文章数量
    private final Map<String, Map<LocalDate, Integer>> domainDateNewsCount = new ConcurrentHashMap<>();
    
    // 用于记录每个域名在不同日期各分类已分配的文章数量
    private final Map<String, Map<LocalDate, Map<String, Integer>>> domainDateCategoryCount = new ConcurrentHashMap<>();
    
    // 线程池配置
    @Value("${domain.assign.thread.core-pool-size:4}")
    private int corePoolSize;
    
    @Value("${domain.assign.thread.max-pool-size:8}")
    private int maxPoolSize;
    
    @Value("${domain.assign.thread.queue-capacity:100}")
    private int queueCapacity;
    
    private ThreadPoolExecutor executor;
    
    @PostConstruct
    public void init() {
        executor = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 根据文章部署日期、分类和域名分配额度进行排序
     */
    private List<DomainConfig> sortDomainsForDateAndCategory(List<DomainConfig> domains, LocalDate deployDate, String categoryName) {
        return domains.stream()
            .filter(domain -> isDomainAvailableForDate(domain, deployDate))
            .sorted((d1, d2) -> {
                // 首先比较总体分配率
                double ratio1 = calculateAssignmentRatioForDate(d1, deployDate);
                double ratio2 = calculateAssignmentRatioForDate(d2, deployDate);
                if (Math.abs(ratio1 - ratio2) > 0.1) { // 如果总体分配率差异大于10%
                    return Double.compare(ratio1, ratio2);
                }
                // 总体分配率接近时，比较分类分配率
                double catRatio1 = calculateCategoryRatioForDate(d1.getDomain(), categoryName, deployDate);
                double catRatio2 = calculateCategoryRatioForDate(d2.getDomain(), categoryName, deployDate);
                return Double.compare(catRatio1, catRatio2);
            })
            .collect(Collectors.toList());
    }

    /**
     * 检查域名在指定日期是否还可以分配文章
     */
    private boolean isDomainAvailableForDate(DomainConfig domain, LocalDate date) {
        int currentCount = getCurrentAssignedCountForDate(domain.getDomain(), date);
        return currentCount < domain.getDailyAddNewsCount();
    }

    /**
     * 获取域名在指定日期已分配的文章数量
     */
    private int getCurrentAssignedCountForDate(String domain, LocalDate date) {
        Map<LocalDate, Integer> dateCountMap = domainDateNewsCount.computeIfAbsent(domain, k -> new ConcurrentHashMap<>());
        int cacheCount = dateCountMap.getOrDefault(date, 0);
        
        int dbCount = newsService.countNewsByDomainAndDate(domain, date);
        
        if (dbCount > cacheCount) {
            dateCountMap.put(date, dbCount);
            return dbCount;
        }
        
        return cacheCount;
    }

    /**
     * 获取域名在指定日期和分类已分配的文章数量
     */
    private int getCurrentAssignedCountForDateAndCategory(String domain, String categoryName, LocalDate date) {
        Map<LocalDate, Map<String, Integer>> dateCategoryMap = domainDateCategoryCount
            .computeIfAbsent(domain, k -> new ConcurrentHashMap<>());
        Map<String, Integer> categoryCountMap = dateCategoryMap
            .computeIfAbsent(date, k -> new ConcurrentHashMap<>());
        
        int cacheCount = categoryCountMap.getOrDefault(categoryName, 0);
        int dbCount = newsService.countNewsByDomainAndDateAndCategory(domain, date, categoryName);
        
        if (dbCount > cacheCount) {
            categoryCountMap.put(categoryName, dbCount);
            return dbCount;
        }
        
        return cacheCount;
    }

    /**
     * 计算域名在指定日期的总体分配比率
     */
    private double calculateAssignmentRatioForDate(DomainConfig domain, LocalDate date) {
        int currentCount = getCurrentAssignedCountForDate(domain.getDomain(), date);
        return (double) currentCount / domain.getDailyAddNewsCount();
    }

    /**
     * 计算域名在指定日期和分类的分配比率
     */
    private double calculateCategoryRatioForDate(String domain, String categoryName, LocalDate date) {
        int totalCount = getCurrentAssignedCountForDate(domain, date);
        if (totalCount == 0) return 0.0;
        
        int categoryCount = getCurrentAssignedCountForDateAndCategory(domain, categoryName, date);
        return (double) categoryCount / totalCount;
    }

    /**
     * 每5分钟执行一次域名分配
     */
    @Scheduled(fixedRate = 300000)
    @Transactional(rollbackFor = Exception.class)
    public void assignDomains() {
        try {
            List<DomainConfig> domainConfigs = domainConfigService.findAllEnabled();
            if (domainConfigs.isEmpty()) {
                log.warn("没有找到启用的域名配置");
                return;
            }
            
            List<News> unassignedNews = newsService.findUnassignedNews();
            if (unassignedNews.isEmpty()) {
                log.debug("没有需要分配域名的文章");
                return;
            }
            
            log.info("开始分配域名，待分配文章数：{}，可用域名数：{}", unassignedNews.size(), domainConfigs.size());
            
            // 按部署日期和分类对文章进行分组
            Map<LocalDate, Map<String, List<News>>> newsByDateAndCategory = unassignedNews.stream()
                .collect(Collectors.groupingBy(
                    news -> news.getPublishTime().toLocalDate(),
                    Collectors.groupingBy(News::getCategoryName)
                ));
            
            // 创建所有任务的Future列表
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            // 对每个部署日期的文章进行分配
            for (Map.Entry<LocalDate, Map<String, List<News>>> dateEntry : newsByDateAndCategory.entrySet()) {
                LocalDate deployDate = dateEntry.getKey();
                Map<String, List<News>> categorizedNews = dateEntry.getValue();
                
                // 对每个分类的文章创建异步任务
                for (Map.Entry<String, List<News>> categoryEntry : categorizedNews.entrySet()) {
                    String categoryName = categoryEntry.getKey();
                    List<News> newsInCategory = categoryEntry.getValue();
                    
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            processNewsCategory(deployDate, categoryName, newsInCategory, domainConfigs);
                        } catch (Exception e) {
                            log.error("处理分类{}的文章时发生错误", categoryName, e);
                            throw e;
                        }
                    }, executor);
                    
                    futures.add(future);
                }
            }
            
            // 等待所有任务完成
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.MINUTES);
                log.info("域名分配完成");
            } catch (Exception e) {
                log.error("等待任务完成时发生错误", e);
                throw new RuntimeException("域名分配失败", e);
            }
            
        } catch (Exception e) {
            log.error("域名分配任务执行失败", e);
            throw e;
        }
    }
    
    /**
     * 处理单个分类的文章分配
     */
    private void processNewsCategory(LocalDate deployDate, String categoryName, 
                                   List<News> newsInCategory, List<DomainConfig> domainConfigs) {
        // 使用同步块确保域名选择的原子性
        synchronized (this) {
            // 按分配比率排序域名（考虑总体分配率和分类分配率）
            List<DomainConfig> sortedDomains = sortDomainsForDateAndCategory(domainConfigs, deployDate, categoryName);
            
            // 分配该分类的文章
            for (News news : newsInCategory) {
                DomainConfig selectedDomain = selectBestDomain(sortedDomains);
                if (selectedDomain == null) {
                    log.warn("所有域名在{}的{}分类都已达到文章限制", deployDate, categoryName);
                    break;
                }
                
                // 分配域名
                assignNewsToDomain(news, selectedDomain, deployDate, categoryName);
                
                // 重新排序域名列表
                sortedDomains = sortDomainsForDateAndCategory(domainConfigs, deployDate, categoryName);
            }
        }
    }

    /**
     * 选择最佳域名进行分配
     */
    private DomainConfig selectBestDomain(List<DomainConfig> sortedDomains) {
        return sortedDomains.isEmpty() ? null : sortedDomains.get(0);
    }

    /**
     * 将文章分配给指定域名
     */
    private void assignNewsToDomain(News news, DomainConfig domain, LocalDate deployDate, String categoryName) {
        news.setDomainConfig(domain.getDomain());
        news.setUpdateTime(LocalDateTime.now());
        newsService.updateDomain(news);
        
        // 更新计数器
        updateDomainStats(domain.getDomain(), deployDate, categoryName);
    }
    
    /**
     * 更新域名统计信息
     */
    private void updateDomainStats(String domain, LocalDate date, String categoryName) {
        // 更新总计数
        Map<LocalDate, Integer> dateCountMap = domainDateNewsCount.computeIfAbsent(domain, k -> new ConcurrentHashMap<>());
        int currentCount = dateCountMap.getOrDefault(date, 0);
        dateCountMap.put(date, currentCount + 1);
        
        // 更新分类计数
        Map<LocalDate, Map<String, Integer>> dateCategoryMap = domainDateCategoryCount
            .computeIfAbsent(domain, k -> new ConcurrentHashMap<>());
        Map<String, Integer> categoryCountMap = dateCategoryMap
            .computeIfAbsent(date, k -> new ConcurrentHashMap<>());
        int currentCategoryCount = categoryCountMap.getOrDefault(categoryName, 0);
        categoryCountMap.put(categoryName, currentCategoryCount + 1);
    }
} 
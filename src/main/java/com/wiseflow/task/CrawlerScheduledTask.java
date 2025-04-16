package com.wiseflow.task;

import com.wiseflow.core.crawler.NetEaseCrawler;
import com.wiseflow.model.CrawlConfig;
import com.wiseflow.model.ParseRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 爬虫定时任务
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class CrawlerScheduledTask {

    private final NetEaseCrawler netEaseCrawler;
    private final RestTemplate restTemplate;
    
    @Value("${wiseflow.api.base-url:http://localhost:8080}")
    private String apiBaseUrl;
    
    // 网易新闻分类URL映射
    private static final Map<String, String> NETEASE_CATEGORY_URLS = new HashMap<>();
    
    static {
        NETEASE_CATEGORY_URLS.put("NBA", "https://sports.163.com/nba/");
        NETEASE_CATEGORY_URLS.put("CBA", "https://sports.163.com/cba/");
        NETEASE_CATEGORY_URLS.put("英超", "https://sports.163.com/yc/");
        NETEASE_CATEGORY_URLS.put("欧冠", "https://sports.163.com/gjb/");
        NETEASE_CATEGORY_URLS.put("西甲", "https://sports.163.com/xj/");
        NETEASE_CATEGORY_URLS.put("意甲", "https://sports.163.com/yj/");
        NETEASE_CATEGORY_URLS.put("法甲", "https://sports.163.com/fj/");
        NETEASE_CATEGORY_URLS.put("德甲", "https://sports.163.com/dj/");
        NETEASE_CATEGORY_URLS.put("中国足球", "https://sports.163.com/china");
        NETEASE_CATEGORY_URLS.put("亚冠", "https://sports.163.com/acl/");
        NETEASE_CATEGORY_URLS.put("国字号", "https://sports.163.com/gjd/");
        NETEASE_CATEGORY_URLS.put("赛车", "https://sports.163.com/f1/");
        NETEASE_CATEGORY_URLS.put("乒乓球", "https://sports.163.com/ppq/");
        NETEASE_CATEGORY_URLS.put("羽毛球", "https://sports.163.com/ymq/");
        NETEASE_CATEGORY_URLS.put("排球", "https://sports.163.com/pq/");
        NETEASE_CATEGORY_URLS.put("田径", "https://sports.163.com/tj/");
        NETEASE_CATEGORY_URLS.put("台球", "https://sports.163.com/ding/");
        NETEASE_CATEGORY_URLS.put("游泳", "https://sports.163.com/youyong/");
    }
    
    /**
     * 每小时执行一次爬虫任务
     */
    @Scheduled(fixedDelay = 30000)
    public void crawlNetEaseNews() {
        log.info("开始执行网易新闻爬虫定时任务");
        
        int totalCount = 0;
        
        // 遍历所有分类进行爬取
        for (Map.Entry<String, String> entry : NETEASE_CATEGORY_URLS.entrySet()) {
            String categoryName = entry.getKey();
            String url = entry.getValue();
            
            try {
                // 创建爬虫配置
                CrawlConfig config = new CrawlConfig();
                config.setUrl(url);
                
                // 设置解析规则
                ParseRule parseRule = new ParseRule();
                parseRule.setCategoryName(categoryName);
                config.setParseRule(parseRule);
                
                // 执行爬虫
                int count = netEaseCrawler.crawl(config);
                totalCount += count;
                
                log.info("分类 [{}] 爬取完成，成功爬取 {} 条新闻", categoryName, count);
                
                // 休眠一段时间，避免请求过于频繁
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("爬取分类 [{}] 失败", categoryName, e);
            }
        }
        
        log.info("网易新闻爬虫定时任务执行完成，共爬取 {} 条新闻", totalCount);
        
        // 清理已爬取的URL缓存
        netEaseCrawler.clearCrawledUrls();
    }
    
    /**
     * 每天凌晨2点执行一次全量爬取
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void crawlNetEaseNewsFullScan() {
        log.info("开始执行网易新闻全量爬取任务");
        
        // 这里可以实现更深度的爬取，例如爬取更多页面或更多分类
        // 为简化示例，这里只是调用普通爬取方法
        crawlNetEaseNews();
        
        log.info("网易新闻全量爬取任务执行完成");
    }
    
    /**
     * 通过API接口获取最新新闻列表
     * @param categoryName 分类名称，可选
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @return 新闻列表
     */
    public List<Map<String, Object>> getLatestNews(String categoryName, int pageNum, int pageSize) {
        String url = apiBaseUrl + "/api/news/list";
        
        // 构建查询参数
        StringBuilder queryParams = new StringBuilder("?pageNum=").append(pageNum)
                .append("&pageSize=").append(pageSize);
        
        if (categoryName != null && !categoryName.isEmpty()) {
            queryParams.append("&categoryName=").append(categoryName);
        }
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url + queryParams, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                if (data.containsKey("list")) {
                    return (List<Map<String, Object>>) data.get("list");
                }
            }
            
            log.error("获取新闻列表失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用新闻列表API失败", e);
        }
        
        return List.of(); // 返回空列表
    }
    
    /**
     * 通过API接口获取新闻详情
     * @param newsId 新闻ID
     * @return 新闻详情
     */
    public Map<String, Object> getNewsDetail(Integer newsId) {
        if (newsId == null) {
            return Map.of();
        }
        
        String url = apiBaseUrl + "/api/news/detail/" + newsId;
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (Map<String, Object>) responseBody.get("data");
            }
            
            log.error("获取新闻详情失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用新闻详情API失败: {}", newsId, e);
        }
        
        return Map.of(); // 返回空Map
    }
    
    /**
     * 通过API接口获取热门新闻
     * @param limit 限制数量，默认5
     * @return 热门新闻列表
     */
    public List<Map<String, Object>> getHotNews(int limit) {
        String url = apiBaseUrl + "/api/news/hot?limit=" + limit;
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();
            
            if (responseBody != null && responseBody.containsKey("data")) {
                return (List<Map<String, Object>>) responseBody.get("data");
            }
            
            log.error("获取热门新闻失败，API返回格式不正确");
        } catch (Exception e) {
            log.error("调用热门新闻API失败", e);
        }
        
        return List.of(); // 返回空列表
    }
} 
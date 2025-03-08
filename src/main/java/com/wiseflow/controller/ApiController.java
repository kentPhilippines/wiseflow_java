package com.wiseflow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.News;
import com.wiseflow.service.ApiService;
import com.wiseflow.service.CategoryService;
import com.wiseflow.service.NewsService;
import com.wiseflow.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API控制器，提供RESTful API接口
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final NewsService newsService;
    private final CategoryService categoryService;
    private final TagService tagService;
    
    /**
     * 获取新闻列表
     */
    @GetMapping("/news/list")
    @Cacheable(value = CacheConfig.CACHE_NEWS_LIST, key = "'categoryName:' + #categoryName + ':pageNum:' + #pageNum + ':pageSize:' + #pageSize + ':all:' + #all")
    public ResponseEntity<Map<String, Object>> getNewsList(
            @RequestParam(required = false) String categoryName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        
        log.info("获取新闻列表: categoryName={}, pageNum={}, pageSize={}, all={}", categoryName, pageNum, pageSize, all);
        
        Page<News> pageRequest = new Page<>(pageNum, pageSize);
        pageRequest.addOrder(OrderItem.desc("publish_time"));
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        if (all) {
            // 全量获取新闻
            IPage<News> newsPage = newsService.findAll(pageRequest);
            data.put("list", newsPage.getRecords().stream()
                    .map(this::convertNewsToMap)
                    .collect(Collectors.toList()));
            data.put("total", newsPage.getTotal());
            data.put("pages", newsPage.getPages());
        } else if (categoryName != null && !categoryName.isEmpty()) {
            // 按分类查询，使用原有方法
            IPage<News> newsPage = newsService.findByCategoryName(categoryName, pageRequest);
            data.put("list", newsPage.getRecords().stream()
                    .map(this::convertNewsToMap)
                    .collect(Collectors.toList()));
            data.put("total", newsPage.getTotal());
            data.put("pages", newsPage.getPages());
        } else {
            // 没有关键字的情况下，按照时间和热度交叉排序
            IPage<News> newsPage = newsService.findHotAndRecentNews(pageRequest);
            data.put("list", newsPage.getRecords().stream()
                    .map(this::convertNewsToMap)
                    .collect(Collectors.toList()));
            data.put("total", newsPage.getTotal());
            data.put("pages", newsPage.getPages());
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取新闻详情
     */
    @GetMapping("/news/detail/{id}")
    public ResponseEntity<Map<String, Object>> getNewsDetail(@PathVariable Integer id) {
        log.info("获取新闻详情: id={}", id);
        
        // 使用findDetailById方法获取包含所有关联数据的新闻详情
        Optional<News> newsOptional = newsService.findDetailById(id);
        
        if (newsOptional.isPresent()) {
            News news = newsOptional.get();
            // 增加浏览次数
            newsService.incrementViewCount(id);
            

            Integer viewCount = news.getViewCount();


            newsService.isHotNews(id);
            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", getNewsDetailWithCache(news));
            
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 404);
            response.put("message", "新闻不存在");
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 获取新闻详情并缓存结果
     * 在增加浏览量后调用此方法，确保浏览量更新不受缓存影响
     */
    @Cacheable(value = CacheConfig.CACHE_NEWS_DETAIL_API, key = "#news.id")
    public Map<String, Object> getNewsDetailWithCache(News news) {
        return convertNewsToDetailMap(news);
    }
    
    /**
     * 获取热门新闻
     */
    @GetMapping("/news/hot")
    @Cacheable(value = CacheConfig.CACHE_HOT_NEWS, key = "'limit:' + #limit + ':all:' + #all")
    public ResponseEntity<Map<String, Object>> getHotNews(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        
        log.info("获取热门新闻: limit={}, all={}", limit, all);
        
        List<News> hotNews;
        if (all) {
            // 获取所有热门新闻
            Page<News> pageRequest = new Page<>(1, Integer.MAX_VALUE);
            hotNews = newsService.findHotNews(pageRequest);
        } else {
            // 获取限定数量的热门新闻
            Page<News> pageRequest = new Page<>(1, limit);
            hotNews = newsService.findHotNews(pageRequest);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", hotNews.stream()
                .map(this::convertNewsToMap)
                .collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取分类列表
     */
    @GetMapping("/category/list")
    @Cacheable(value = CacheConfig.CACHE_CATEGORY_LIST)
    public ResponseEntity<Map<String, Object>> getCategoryList() {
        log.info("获取分类列表");
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", categoryService.findAllWithNewsCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取标签列表
     */
    @GetMapping("/tag/list")
    @Cacheable(value = CacheConfig.CACHE_TAG_LIST, key = "'limit:' + #limit")
    public ResponseEntity<Map<String, Object>> getTagList(@RequestParam(defaultValue = "20") int limit) {
        log.info("获取标签列表: limit={}", limit);
        
        Page<com.wiseflow.entity.Tag> pageRequest = new Page<>(1, limit);
        pageRequest.addOrder(OrderItem.desc("frequency"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", tagService.findAll(pageRequest).getRecords());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 搜索新闻
     */
    @GetMapping("/news/search")
    @Cacheable(value = CacheConfig.CACHE_SEARCH_RESULT, key = "'keyword:' + #keyword + ':pageNum:' + #pageNum + ':pageSize:' + #pageSize + ':all:' + #all")
    public ResponseEntity<Map<String, Object>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        
        log.info("搜索新闻: keyword={}, pageNum={}, pageSize={}, all={}", keyword, pageNum, pageSize, all);
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        if (all) {
            // 全量搜索，不分页
            IPage<News> newsPage = newsService.search(keyword, new Page<>(1, Integer.MAX_VALUE));
            data.put("list", newsPage.getRecords().stream()
                    .map(this::convertNewsToMap)
                    .collect(Collectors.toList()));
            data.put("total", newsPage.getTotal());
            data.put("pages", 1);
        } else {
            // 分页搜索
            Page<News> pageRequest = new Page<>(pageNum, pageSize);
            IPage<News> newsPage = newsService.search(keyword, pageRequest);
            data.put("list", newsPage.getRecords().stream()
                    .map(this::convertNewsToMap)
                    .collect(Collectors.toList()));
            data.put("total", newsPage.getTotal());
            data.put("pages", newsPage.getPages());
        }
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取指定分类的新闻数量
     */
    @GetMapping("/category/{id}/count")
    @Cacheable(value = CacheConfig.CACHE_CATEGORY_NEWS_COUNT, key = "#id")
    public ResponseEntity<Map<String, Object>> getCategoryNewsCount(@PathVariable Integer id) {
        log.info("获取分类新闻数量: id={}", id);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 先检查分类是否存在
            categoryService.findById(id);
            
            Integer count = categoryService.getNewsCountByCategoryId(id);
            
            response.put("code", 200);
            response.put("message", "success");
            response.put("data", count);
        } catch (NoSuchElementException e) {
            response.put("code", 404);
            response.put("message", "分类不存在");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 将News对象转换为Map（列表展示用）
     * 使用本地缓存提高性能
     */
    @Cacheable(value = "newsListMap", key = "#news.id")
    public Map<String, Object> convertNewsToMap(News news) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", news.getId());
        map.put("title", news.getTitle());
        map.put("categoryName", news.getCategoryName());
        map.put("publishTime", news.getPublishTime());
        map.put("viewCount", news.getViewCount());
        map.put("coverImage", news.getCoverImage());


        // 获取摘要
        if (news.getContent() != null) {
            map.put("summary", news.getContent().getSummary());
        }
        
        return map;
    }
    
    /**
     * 将News对象转换为详情Map（详情展示用）
     */
    private Map<String, Object> convertNewsToDetailMap(News news) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", news.getId());
        map.put("title", news.getTitle());
        map.put("subtitle", news.getSubtitle());
        map.put("categoryId", news.getCategoryId());
        map.put("categoryName", news.getCategoryName());
        map.put("author", news.getAuthor());
        map.put("source", news.getSource());
        map.put("url", news.getUrl());
        map.put("publishTime", news.getPublishTime());
        map.put("viewCount", news.getViewCount());
        map.put("commentCount", news.getCommentCount());
        map.put("likeCount", news.getLikeCount());
        
        // 内容
        if (news.getContent() != null) {
            map.put("content", news.getContent().getContent());
            map.put("contentHtml", news.getContent().getContentHtml());
            map.put("summary", news.getContent().getSummary());
        }
        
        // 图片
        if (news.getImages() != null && !news.getImages().isEmpty()) {
            map.put("images", news.getImages().stream()
                .map(image -> {
                    Map<String, Object> imageMap = new HashMap<>();
                    imageMap.put("url", image.getUrl());
                    imageMap.put("position", image.getPosition());
                    imageMap.put("isCover", image.getIsCover());
                    return imageMap;
                })
                .collect(Collectors.toList()));
        } else {
            map.put("images", List.of());
        }
        
        // 标签
        if (news.getTags() != null && !news.getTags().isEmpty()) {
            map.put("tags", news.getTags().stream()
                .map(tag -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("id", tag.getId());
                    tagMap.put("name", tag.getName());
                    return tagMap;
                })
                .collect(Collectors.toList()));
        } else {
            map.put("tags", List.of());
        }
        
        return map;
    }
} 
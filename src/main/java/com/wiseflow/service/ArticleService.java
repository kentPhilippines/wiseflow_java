package com.wiseflow.service;

import com.wiseflow.core.storage.Storage;
import com.wiseflow.entity.Article;
import com.wiseflow.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.Hibernate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final Storage storage;
    private final Set<String> urlCache = Collections.synchronizedSet(new HashSet<>());
    
    @PostConstruct
    public void init() {
        // 启动时加载所有URL到缓存
        try {
            List<String> urls = articleRepository.findAllUrls();
            urlCache.addAll(urls);
            log.info("Loaded {} URLs into cache", urls.size());
        } catch (Exception e) {
            log.error("Failed to initialize URL cache", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean isUrlExists(String url) {
        if (url == null) {
            return false;
        }
        
        // 先检查缓存
        if (urlCache.contains(url)) {
            log.debug("URL found in cache: {}", url);
            return true;
        }
        
        // 再检查数据库
        boolean exists = articleRepository.existsByUrl(url);
        if (exists) {
            // 如果数据库中存在但缓存中没有，更新缓存
            urlCache.add(url);
            log.debug("URL found in database and added to cache: {}", url);
        }
        
        return exists;
    }

    @Transactional(readOnly = true)
    public Page<Article> findAll(Pageable pageable) {
        return articleRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Article findById(Long id) {
        return articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Article not found"));
    }

    @Transactional(readOnly = true)
    public Page<Article> findByCategory(Long categoryId, Pageable pageable) {
        return articleRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable);
    }

    @Transactional
    public Article save(Article article) {
        try {
            if (isUrlExists(article.getUrl())) {
                log.debug("Article with URL {} already exists", article.getUrl());
                return null;
            }

            // 设置创建时间
            if (article.getCreatedAt() == null) {
                article.setCreatedAt(LocalDateTime.now());
            }
            
            // 设置抓取时间
            if (article.getCrawlTime() == null) {
                article.setCrawlTime(LocalDateTime.now());
            }

            // 生成摘要
            if (article.getSummary() == null) {
                article.setSummaryFromContent();
            }

            // 保存到数据库
            Article savedArticle = articleRepository.save(article);
            urlCache.add(article.getUrl());
            
            log.info("Saved article to database: {} ({})", article.getTitle(), article.getUrl());
            return savedArticle;
        } catch (Exception e) {
            log.error("Failed to save article: {} ({})", article.getTitle(), article.getUrl(), e);
            throw new RuntimeException("Failed to save article", e);
        }
    }

    @Transactional
    public void deleteExpiredArticles(int days) {
        try {
            LocalDateTime expireDate = LocalDateTime.now().minusDays(days);
            log.info("Starting to delete articles older than {}", expireDate);
            
            // 使用 JPA 的分页功能批量处理，避免一次加载太多数据
            int pageSize = 100;
            int page = 0;
            int totalDeleted = 0;
            
            while (true) {
                PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by("createdAt"));
                Page<Article> articles = articleRepository.findByCreatedAtBefore(expireDate, pageRequest);
                
                if (articles.isEmpty()) {
                    break;
                }
                
                for (Article article : articles) {
                    try {
                        // 从文件系统删除
                        storage.delete(article);
                        // 从数据库删除
                        articleRepository.delete(article);
                        // 从URL缓存中移除
                        urlCache.remove(article.getUrl());
                        
                        totalDeleted++;
                        log.debug("Deleted expired article: {} ({})", article.getTitle(), article.getUrl());
                    } catch (Exception e) {
                        log.error("Failed to delete article: {} ({})", article.getTitle(), article.getUrl(), e);
                    }
                }
                
                // 清理持久化上下文，避免内存占用过大
                articleRepository.flush();
                
                if (!articles.hasNext()) {
                    break;
                }
                
                page++;
            }
            
            log.info("Successfully deleted {} expired articles", totalDeleted);
        } catch (Exception e) {
            log.error("Failed to delete expired articles", e);
            throw new RuntimeException("Failed to delete expired articles", e);
        }
    }

    @Transactional
    public void deleteArticle(Long id) {
        Article article = findById(id);
        storage.delete(article);
        articleRepository.delete(article);
        urlCache.remove(article.getUrl());
    }

    @Transactional(readOnly = true)
    public Page<Article> searchByKeyword(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getArticleStats(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return articleRepository.getArticleStatsByDate(startDate);
    }

    @Transactional(readOnly = true)
    public List<Article> findUnsyncedArticles() {
        return articleRepository.findBySyncedFalseOrderByCreatedAtAsc();
    }
} 
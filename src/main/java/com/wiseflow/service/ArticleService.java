package com.wiseflow.service;

import com.wiseflow.entity.Article;
import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService {
    
    /**
     * 根据ID查找文章
     * @param id 文章ID
     * @return 文章对象
     */
    Article findById(Long id);
    
    /**
     * 保存文章
     * @param article 文章对象
     * @return 保存后的文章
     */
    Article saveArticle(Article article);
    
    /**
     * 删除文章
     * @param id 文章ID
     */
    void delete(Long id);
    
    /**
     * 获取所有文章
     * @return 文章列表
     */
    List<Article> findAll();
    
    /**
     * 根据域名获取文章
     * @param domain 域名
     * @return 文章列表
     */
    List<Article> findByDomain(String domain);
    
    /**
     * 根据分类ID获取文章
     * @param categoryId 分类ID
     * @return 文章列表
     */
    List<Article> findByCategoryId(Integer categoryId);
    
    /**
     * 获取需要改写的文章列表
     * 
     * @param domain 域名
     * @param limit 限制数量
     * @return 需要改写的文章列表
     */
    List<Article> getArticlesToRewrite(String domain, int limit);
    
    /**
     * 更新文章改写状态
     * 
     * @param articleId 文章ID
     * @param isRewritten 是否已改写
     */
    void updateArticleRewriteStatus(Long articleId, boolean isRewritten);
} 
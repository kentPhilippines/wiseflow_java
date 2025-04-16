package com.wiseflow.service;

import com.wiseflow.entity.ArticleRule;
import java.util.List;

/**
 * 文章分配规则Service接口
 */
public interface ArticleRuleService {
    
    /**
     * 获取所有规则
     */
    List<ArticleRule> findAll();
    
    /**
     * 根据ID获取规则
     */
    ArticleRule findById(Long id);
    
    /**
     * 保存规则
     */
    boolean save(ArticleRule rule);
    
    /**
     * 更新规则
     */
    ArticleRule update(ArticleRule rule);
    
    /**
     * 删除规则
     */
    void delete(Long id);

    List<ArticleRule> getArticlesByDomain(String domain);

    long count();

}
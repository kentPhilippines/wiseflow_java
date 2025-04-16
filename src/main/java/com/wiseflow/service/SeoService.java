package com.wiseflow.service;

import com.wiseflow.entity.News;
import com.wiseflow.entity.SeoKeyword;
import java.util.List;

public interface SeoService {
    
    /**
     * 获取域名下的所有启用的关键词
     */
    List<SeoKeyword> getEnabledKeywords(String domain);
    
    /**
     * 处理文章SEO优化
     * @param news 待处理的文章
     * @return 处理后的文章
     */
    News processSeoOptimization(News news);
    
    /**
     * 保存关键词
     */
    SeoKeyword saveKeyword(SeoKeyword keyword);
    
    /**
     * 删除关键词
     */
    void deleteKeyword(Integer id);
} 
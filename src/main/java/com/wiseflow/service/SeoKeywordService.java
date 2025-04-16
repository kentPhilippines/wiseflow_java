package com.wiseflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wiseflow.entity.SeoKeyword;
import java.util.List;

/**
 * SEO关键词服务接口
 */
public interface SeoKeywordService extends IService<SeoKeyword> {
    
    /**
     * 获取所有关键词
     */
    List<SeoKeyword> findAll();
    
    /**
     * 保存关键词
     */
    SeoKeyword save1(SeoKeyword keyword);
    
    /**
     * 批量保存关键词
     */
    List<SeoKeyword> batchSave(List<SeoKeyword> keywords);
    
    /**
     * 更新关键词
     */
    void update(SeoKeyword keyword);
    
    /**
     * 删除关键词
     */
    void delete(Long id);
    
    /**
     * 批量删除关键词
     */
    void batchDelete(List<Long> ids);
    
    /**
     * 批量更新状态
     */
    void batchUpdateStatus(List<Long> ids, Boolean enabled);
    
    /**
     * 获取启用的关键词
     */
    List<SeoKeyword> findEnabled();
    
    /**
     * 根据类型获取关键词
     */
    List<SeoKeyword> findByType(Integer type);
    
    /**
     * 根据使用场景获取关键词
     */
    List<SeoKeyword> findByUseScene(Integer useScene);
    
    /**
     * 根据ID获取关键词
     */
    SeoKeyword findById(Long id);
    
    /**
     * 根据状态获取关键词
     */
    List<SeoKeyword> findByStatus(Boolean enabled);

    List<SeoKeyword> getKeywordsByDomain(String domain);
} 
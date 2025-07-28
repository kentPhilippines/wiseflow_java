package com.wiseflow.service;

import com.wiseflow.vo.SeoKeywordVO;
import com.wiseflow.vo.ApiResult;

import java.util.List;

/**
 * 域名SEO关键词服务接口
 */
public interface DomainSeoKeywordService {
    
    /**
     * 根据域名ID获取关键词列表
     * 
     * @param domainId 域名ID
     * @return 关键词列表
     */
    List<SeoKeywordVO> getKeywordsByDomainId(Integer domainId);
    
    /**
     * 保存域名的SEO关键词
     * 
     * @param seoKeywordVO SEO关键词VO
     * @return API结果
     */
    ApiResult saveDomainKeyword(SeoKeywordVO seoKeywordVO);
    
    /**
     * 批量保存域名的SEO关键词
     * 
     * @param seoKeywordVO SEO关键词VO（包含多个域名ID）
     * @return API结果
     */
    ApiResult batchSaveDomainKeywords(SeoKeywordVO seoKeywordVO);
    
    /**
     * 删除域名的SEO关键词
     * 
     * @param domainId 域名ID
     * @param keywordId 关键词ID
     * @return API结果
     */
    ApiResult deleteDomainKeyword(Integer domainId, Integer keywordId);
    
    /**
     * 关联域名和关键词
     * 
     * @param domainId 域名ID
     * @param keywordId 关键词ID
     * @return API结果
     */
    ApiResult associateDomainWithKeyword(Integer domainId, Integer keywordId);
} 
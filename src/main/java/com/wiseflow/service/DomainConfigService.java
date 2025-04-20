package com.wiseflow.service;

import com.wiseflow.entity.*;
import com.wiseflow.dto.DomainConfigRequest;
import java.util.List;
import java.util.Optional;

/**
 * 域名配置服务接口
 */
public interface DomainConfigService {
    
    /**
     * 获取域名配置总数
     *
     * @return 域名配置总数
     */
    Long count();
    
    /**
     * 获取启用的域名配置总数
     *
     * @return 启用的域名配置总数
     */
    Long countEnabled();
    
    /**
     * 保存域名配置
     */
    DomainConfig save(DomainConfig domainConfig);
    
    /**
     * 根据ID查询域名配置
     */
    Optional<DomainConfig> findById(Integer id);
    
    /**
     * 根据域名查询配置
     */
    Optional<DomainConfig> findByDomain(String domain);
    
    /**
     * 检查域名是否存在
     */
    boolean isDomainExists(String domain);
    
    /**
     * 查询所有域名配置
     */
    List<DomainConfig> findAll();
    
    /**
     * 更新域名配置
     */
    DomainConfig update(DomainConfig domainConfig);
    
    /**
     * 删除域名配置
     */
    void deleteById(Integer id);
    
    /**
     * 更新域名配置状态
     * @param id 配置ID
     * @param status 状态：1-启用，0-禁用
     */
    void toggleStatus(Integer id, Integer status);
    
    /**
     * 批量导入域名配置
     * @param domainConfigs 域名配置列表
     * @param overwrite 是否覆盖已存在的配置
     * @return 导入成功的数量
     */
    int batchImport(List<DomainConfig> domainConfigs, boolean overwrite);
    
    /**
     * 保存域名配置模板
     */
    void saveTemplate(DomainConfig template);

    /**
     * 查找所有启用的域名配置
     */
    List<DomainConfig> findAllEnabled();

    /**
     * 保存域名SEO关键词配置
     */
    void saveKeywordConfig(Long domainId, DomainConfigRequest request);

    /**
     * 保存域名评论规则配置
     */
    void saveCommentConfig(Long domainId, DomainConfigRequest request);

    /**
     * 保存域名文章配置
     */
    void saveArticleConfig(Long domainId, DomainConfigRequest request);

    /**
     * 获取所有域名的SEO关键词配置
     */
    List<SeoKeyword> getAllDomainKeywords(DomainConfig domainConfig);

    /**
     * 获取所有域名的评论规则配置
     */
    List<CommentRule> getAllDomainCommentRules(DomainConfig domainConfig);

    /**
     * 获取所有域名的文章配置
     */
    List<ArticleRule> getAllDomainArticleConfigs(DomainConfig domainConfig);

    /**
     * 获取所有启用的域名配置
     * @return 所有启用的域名配置列表
     */
    List<DomainConfig> getEnabledDomainConfigs();
    
    /**
     * 批量生成域名配置
     * 根据域名列表和模板ID生成配置
     *
     * @param domains 域名列表
     * @param templateId 模板ID（可选）
     * @return 成功生成的配置数量
     */
    int batchGenerate(List<String> domains, Long templateId);
} 
package com.wiseflow.service;

import com.wiseflow.entity.*;
import com.wiseflow.dto.DomainConfigRequest;
import com.google.protobuf.ServiceException;
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
     * @throws ServiceException 如果模板不存在或处理过程中出现错误
     */
    int batchGenerate(List<String> domains, Long templateId)  ;

    List<DomainConfig> findAllPhone(String phone, String domain);

    void saveCommentConfig(List<Integer> domainIds, String phone, List<Integer> selectedRules,
            boolean overwriteExisting);

    void saveKeywordConfig(List<Integer> domainIds, String phone, List<Integer> selectedRules,
            boolean overwriteExisting);

    void saveArticleConfig(List<Integer> domainIds, String phone, List<Integer> selectedRules,
            boolean overwriteExisting);

    /**
     * 批量删除指定手机号下的域名配置
     * 删除逻辑：
     * 1. 获取到这个批次的所有域名
     * 2. 根据域名查询域名配置【关键词，评论，文章】并删除配置内容
     * 3. 删除域名配置
     * 4. 删除文章和域名的绑定关系
     * 5. 删除域名
     * 
     * @param phone 手机号（批次号）
     * @param domainIds 要删除的域名配置ID列表，如果为空则删除该手机号下所有域名
     * @return 删除结果，包含成功删除的数量等信息
     */
    BatchDeleteResult batchDeleteByPhone(String phone, List<Integer> domainIds);

    /**
     * 批量删除结果
     */
    class BatchDeleteResult {
        private boolean success;
        private int deletedCount;
        private String message;
        private List<String> deletedDomains;
        private List<String> failedDomains;

        public BatchDeleteResult() {}

        public BatchDeleteResult(boolean success, int deletedCount, String message) {
            this.success = success;
            this.deletedCount = deletedCount;
            this.message = message;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getDeletedCount() { return deletedCount; }
        public void setDeletedCount(int deletedCount) { this.deletedCount = deletedCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getDeletedDomains() { return deletedDomains; }
        public void setDeletedDomains(List<String> deletedDomains) { this.deletedDomains = deletedDomains; }
        
        public List<String> getFailedDomains() { return failedDomains; }
        public void setFailedDomains(List<String> failedDomains) { this.failedDomains = failedDomains; }
    }
}
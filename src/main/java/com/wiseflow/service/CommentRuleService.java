package com.wiseflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wiseflow.entity.CommentRule;
import java.util.List;

/**
 * 评论规则服务接口
 */
public interface CommentRuleService extends IService<CommentRule> {
    
    /**
     * 获取评论规则总数
     *
     * @return 评论规则总数
     */
    long count();
    
    /**
     * 根据ID获取评论规则
     * @param id 规则ID
     * @return 评论规则
     */
    CommentRule getById(Long id);
    
    /**
     * 保存评论规则
     * @param rule 评论规则
     * @return 是否保存成功
     */
    boolean saveRule(CommentRule rule);
    
    /**
     * 更新评论规则
     * @param rule 评论规则
     * @return 是否更新成功
     */
    boolean updateRule(CommentRule rule);
    
    /**
     * 删除评论规则
     * @param id 规则ID
     * @return 是否删除成功
     */
    boolean deleteRule(Long id);
    
    /**
     * 根据域名配置查找评论规则
     */
    List<CommentRule> findByDomainConfig(String domainConfig);
    
    /**
     * 初始化默认评论规则
     */
    List<CommentRule>  initDefaultRules(String domainConfig);
    
    /**
     * 获取所有启用的评论规则
     */
    List<CommentRule> findAllEnabled();
    
    /**
     * 获取指定域名配置的所有启用规则
     */
    List<CommentRule> findEnabledByDomainConfig(String domainConfig);
    
    /**
     * 切换规则状态
     */
    void toggleStatus(Integer id, Boolean enabled);
    
    /**
     * 更新规则状态
     */
    void updateStatus(Integer id, Boolean enabled);
    
    /**
     * 查找所有评论规则
     */
    List<CommentRule> findAll();
    
    /**
     * 初始化单个默认规则
     */
    CommentRule initDefaultRule(String domainConfig);
    

    /**
     * 批量更新评论规则状态
     * @param ids 规则ID列表
     * @param enabled 启用状态
     */
    void batchUpdateStatus(List<Integer> ids, Boolean enabled);

    List<CommentRule> getRulesByDomain(String domain);
} 
package com.wiseflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wiseflow.entity.DomainCommentRule;

import java.util.List;

/**
 * 域名评论规则关联服务接口
 */
public interface DomainCommentRuleService extends IService<DomainCommentRule> {
    
    /**
     * 根据域名配置ID获取关联
     * 
     * @param domainConfigId 域名配置ID
     * @return 域名评论规则关联
     */
    DomainCommentRule getByDomainConfigId(Integer domainConfigId);
    
    /**
     * 检查域名是否已有规则
     * 
     * @param domainConfigId 域名配置ID
     * @return 是否已有规则
     */
    boolean hasRule(Integer domainConfigId);
    
    /**
     * 根据规则ID获取所有使用该规则的域名配置ID
     * 
     * @param ruleId 规则ID
     * @return 域名配置ID列表
     */
    List<Integer> getDomainConfigIdsByRuleId(Integer ruleId);
    
    /**
     * 批量保存或更新
     * 
     * @param domainCommentRules 域名评论规则关联列表
     * @return 是否成功
     */
    boolean saveOrUpdateBatch(List<DomainCommentRule> domainCommentRules);
} 
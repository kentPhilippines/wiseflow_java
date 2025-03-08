package com.wiseflow.service;

import com.wiseflow.entity.DomainConfig;

import java.util.List;
import java.util.Optional;

/**
 * 域名配置服务接口
 * 使用MyBatis-Plus实现，不使用事务管理
 */
public interface DomainConfigService {
    
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
     * 启用/禁用域名配置
     */
    void toggleStatus(Integer id, boolean enabled);
} 
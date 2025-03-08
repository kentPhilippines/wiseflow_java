package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.mapper.DomainConfigMapper;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 域名配置服务实现类
 * 使用MyBatis-Plus实现，不使用事务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainConfigServiceImpl implements DomainConfigService {
    
    private final DomainConfigMapper domainConfigMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    public DomainConfig save(DomainConfig domainConfig) {
        // 处理友情链接
        processFriendlyLinks(domainConfig);
        
        if (domainConfig.getId() == null) {
            domainConfigMapper.insert(domainConfig);
        } else {
            domainConfigMapper.updateById(domainConfig);
        }
        return domainConfig;
    }
    
    @Override
    public Optional<DomainConfig> findById(Integer id) {
        DomainConfig domainConfig = domainConfigMapper.selectById(id);
        if (domainConfig != null) {
            // 解析友情链接
            parseFriendlyLinks(domainConfig);
        }
        return Optional.ofNullable(domainConfig);
    }
    
    @Override
    public Optional<DomainConfig> findByDomain(String domain) {
        DomainConfig domainConfig = domainConfigMapper.selectByDomain(domain);
        if (domainConfig != null) {
            // 解析友情链接
            parseFriendlyLinks(domainConfig);
        }
        return Optional.ofNullable(domainConfig);
    }
    
    @Override
    public boolean isDomainExists(String domain) {
        return domainConfigMapper.countByDomain(domain) > 0;
    }
    
    @Override
    public List<DomainConfig> findAll() {
        List<DomainConfig> domainConfigs = domainConfigMapper.selectList(null);
        // 解析所有配置的友情链接
        domainConfigs.forEach(this::parseFriendlyLinks);
        return domainConfigs;
    }
    
    @Override
    public DomainConfig update(DomainConfig domainConfig) {
        // 确保配置存在
        findById(domainConfig.getId()).orElseThrow(() -> 
            new NoSuchElementException("Domain config not found with id: " + domainConfig.getId()));
        
        // 处理友情链接
        processFriendlyLinks(domainConfig);
        
        domainConfigMapper.updateById(domainConfig);
        return domainConfig;
    }
    
    @Override
    public void deleteById(Integer id) {
        domainConfigMapper.deleteById(id);
    }
    
    @Override
    public void toggleStatus(Integer id, boolean enabled) {
        DomainConfig domainConfig = findById(id).orElseThrow(() -> 
            new NoSuchElementException("Domain config not found with id: " + id));
        
        domainConfig.setStatus(enabled ? 1 : 0);
        domainConfigMapper.updateById(domainConfig);
    }
    
    /**
     * 处理友情链接，将对象列表转换为JSON字符串
     */
    private void processFriendlyLinks(DomainConfig domainConfig) {
        if (domainConfig.getFriendlyLinkList() != null && !domainConfig.getFriendlyLinkList().isEmpty()) {
            try {
                domainConfig.setFriendlyLinks(objectMapper.writeValueAsString(domainConfig.getFriendlyLinkList()));
            } catch (JsonProcessingException e) {
                log.error("Error processing friendly links", e);
                domainConfig.setFriendlyLinks("[]");
            }
        } else {
            domainConfig.setFriendlyLinks("[]");
        }
    }
    
    /**
     * 解析友情链接，将JSON字符串转换为对象列表
     */
    private void parseFriendlyLinks(DomainConfig domainConfig) {
        if (domainConfig.getFriendlyLinks() != null && !domainConfig.getFriendlyLinks().isEmpty()) {
            try {
                domainConfig.setFriendlyLinkList(
                    objectMapper.readValue(
                        domainConfig.getFriendlyLinks(), 
                        new TypeReference<List<DomainConfig.FriendlyLink>>() {}
                    )
                );
            } catch (JsonProcessingException e) {
                log.error("Error parsing friendly links", e);
                domainConfig.setFriendlyLinkList(new ArrayList<>());
            }
        } else {
            domainConfig.setFriendlyLinkList(new ArrayList<>());
        }
    }
} 
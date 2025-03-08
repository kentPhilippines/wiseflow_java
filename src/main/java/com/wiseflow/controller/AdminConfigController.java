package com.wiseflow.controller;

import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 配置管理控制器
 * 提供管理域名配置的管理员API接口
 */
@Slf4j
@RestController
@RequestMapping("/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final DomainConfigService domainConfigService;
    
    /**
     * 获取所有域名配置
     */
    @GetMapping("/domain")
    public ResponseEntity<List<DomainConfig>> getAllDomainConfigs() {
        log.info("获取所有域名配置");
        return ResponseEntity.ok(domainConfigService.findAll());
    }
    
    /**
     * 根据ID获取域名配置
     */
    @GetMapping("/domain/{id}")
    public ResponseEntity<DomainConfig> getDomainConfigById(@PathVariable Integer id) {
        log.info("根据ID获取域名配置: id={}", id);
        return ResponseEntity.ok(domainConfigService.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Domain config not found with id: " + id)));
    }
    
    /**
     * 保存域名配置
     * 清除域名配置缓存
     */
    @PostMapping("/domain")
    @CacheEvict(value = CacheConfig.CACHE_DOMAIN_CONFIG, key = "#domainConfig.domain")
    public ResponseEntity<DomainConfig> saveDomainConfig(@RequestBody DomainConfig domainConfig) {
        log.info("保存域名配置: domain={}", domainConfig.getDomain());
        return ResponseEntity.ok(domainConfigService.save(domainConfig));
    }
    
    /**
     * 更新域名配置
     * 清除域名配置缓存
     */
    @PutMapping("/domain/{id}")
    @CacheEvict(value = CacheConfig.CACHE_DOMAIN_CONFIG, allEntries = true)
    public ResponseEntity<DomainConfig> updateDomainConfig(
            @PathVariable Integer id, 
            @RequestBody DomainConfig domainConfig) {
        
        log.info("更新域名配置: id={}, domain={}", id, domainConfig.getDomain());
        
        domainConfig.setId(id);
        return ResponseEntity.ok(domainConfigService.update(domainConfig));
    }
    
    /**
     * 删除域名配置
     * 清除域名配置缓存
     */
    @DeleteMapping("/domain/{id}")
    @CacheEvict(value = CacheConfig.CACHE_DOMAIN_CONFIG, allEntries = true)
    public ResponseEntity<Void> deleteDomainConfig(@PathVariable Integer id) {
        log.info("删除域名配置: id={}", id);
        
        domainConfigService.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 启用/禁用域名配置
     * 清除域名配置缓存
     */
    @PutMapping("/domain/{id}/status")
    @CacheEvict(value = CacheConfig.CACHE_DOMAIN_CONFIG, allEntries = true)
    public ResponseEntity<Void> toggleDomainConfigStatus(
            @PathVariable Integer id, 
            @RequestParam boolean enabled) {
        
        log.info("切换域名配置状态: id={}, enabled={}", id, enabled);
        
        domainConfigService.toggleStatus(id, enabled);
        return ResponseEntity.ok().build();
    }
} 
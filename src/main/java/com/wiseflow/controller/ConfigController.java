package com.wiseflow.controller;

import com.wiseflow.config.CacheConfig;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 配置控制器
 * 提供获取域名配置的公共API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final DomainConfigService domainConfigService;
    
    /**
     * 获取域名配置
     * 包括网站标题、描述、关键词和友情链接等
     */
    @GetMapping("/domain/{domain}")
    public ResponseEntity<Map<String, Object>> getDomainConfig(@PathVariable String domain) {
        log.info("获取域名配置: domain={}", domain);
        
        Optional<DomainConfig> configOptional = domainConfigService.findByDomain(domain);
        
        Map<String, Object> response = new HashMap<>();
        
        if (configOptional.isPresent()) {
            DomainConfig config = configOptional.get();
            
            response.put("code", 200);
            response.put("message", "success");
            
            Map<String, Object> data = new HashMap<>();
            data.put("title", config.getTitle());
            data.put("description", config.getDescription());
            data.put("keywords", config.getKeywords());
            data.put("logoUrl", config.getLogoUrl());
            data.put("faviconUrl", config.getFaviconUrl());
            data.put("copyright", config.getCopyright());
            data.put("icp", config.getIcp());
            data.put("contactPhone", config.getContactPhone());
            data.put("contactEmail", config.getContactEmail());
            data.put("contactAddress", config.getContactAddress());
            data.put("friendlyLinks", config.getFriendlyLinkList());
            data.put("views", config.getViewsPath());
            
            response.put("data", data);
        } else {
            response.put("code", 404);
            response.put("message", "域名配置不存在");
        }
        
        return ResponseEntity.ok(response);
    }


    
} 
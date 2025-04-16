package com.wiseflow.controller;

import com.wiseflow.entity.DomainConfig;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     */
    @PostMapping("/domain")
    public ResponseEntity<DomainConfig> saveDomainConfig(@RequestBody DomainConfig domainConfig) {
        log.info("保存域名配置: domain={}", domainConfig.getDomain());
        return ResponseEntity.ok(domainConfigService.save(domainConfig));
    }
    
    /**
     * 更新域名配置
     */
    @PutMapping("/domain/{id}")
    public ResponseEntity<DomainConfig> updateDomainConfig(
            @PathVariable Integer id, 
            @RequestBody DomainConfig domainConfig) {
        
        log.info("更新域名配置: id={}, domain={}", id, domainConfig.getDomain());
        
        domainConfig.setId(id);
        return ResponseEntity.ok(domainConfigService.update(domainConfig));
    }
    
    /**
     * 删除域名配置
     */
    @DeleteMapping("/domain/{id}")
    public ResponseEntity<Void> deleteDomainConfig(@PathVariable Integer id) {
        log.info("删除域名配置: id={}", id);
        
        domainConfigService.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 更新域名配置状态
     */
    @PutMapping("/domain/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Integer id, @RequestParam Integer status) {
        domainConfigService.toggleStatus(id, status);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 批量导入域名配置
     * 将数组数据转换为一条SQL插入数据库
     */
    @PostMapping("/domain/batch-import")
    public ResponseEntity<Map<String, Object>> batchImportDomainConfigs(
            @RequestBody List<DomainConfig> domainConfigs,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        
        log.info("批量导入域名配置: count={}, overwrite={}", domainConfigs.size(), overwrite);
        
        int importedCount = domainConfigService.batchImport(domainConfigs, overwrite);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "成功导入 " + importedCount + " 条配置");
        response.put("importedCount", importedCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 保存域名配置模板
     */
    @PostMapping("/domain/template")
    public ResponseEntity<Map<String, Object>> saveTemplate(@RequestBody DomainConfig template) {
        log.info("保存域名配置模板");
        
        domainConfigService.saveTemplate(template);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "模板保存成功");
        
        return ResponseEntity.ok(response);
    }
} 
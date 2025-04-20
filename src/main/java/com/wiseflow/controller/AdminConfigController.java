package com.wiseflow.controller;

import com.wiseflow.entity.DomainConfig;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

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
    private final ResourceLoader resourceLoader;
    
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
     * 批量生成域名配置
     * 根据域名列表和模板生成配置
     */
    @PostMapping("/domain/batch-generate")
    public ResponseEntity<Map<String, Object>> batchGenerateDomainConfigs(
            @RequestBody List<String> domains,
            @RequestParam(required = false) Long templateId) {
        
        log.info("批量生成域名配置: count={}, templateId={}", domains.size(), templateId);
        
        int generatedCount = domainConfigService.batchGenerate(domains, templateId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "成功生成 " + generatedCount + " 条配置");
        response.put("generatedCount", generatedCount);
        
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
    
    /**
     * 解析上传的Excel文件并导入域名配置
     */
    @PostMapping("/domain/parse-excel")
    public ResponseEntity<Map<String, Object>> parseExcelAndImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean overwrite) {
        
        log.info("解析Excel文件并导入域名配置: fileName={}, size={}, overwrite={}", 
                file.getOriginalFilename(), file.getSize(), overwrite);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<DomainConfig> domainConfigs = parseExcelFile(file);
            int importedCount = domainConfigService.batchImport(domainConfigs, overwrite);
            
            response.put("code", 200);
            response.put("message", "成功导入 " + importedCount + " 条配置");
            response.put("importedCount", importedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("解析Excel文件失败", e);
            response.put("code", 500);
            response.put("message", "解析Excel文件失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 解析Excel文件为域名配置列表
     */
    private List<DomainConfig> parseExcelFile(MultipartFile file) throws IOException {
        List<DomainConfig> domainConfigs = new ArrayList<>();
        
        Workbook workbook = null;
        try {
            // 根据文件扩展名创建不同的Workbook
            String fileName = file.getOriginalFilename();
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(file.getInputStream());
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(file.getInputStream());
            } else {
                throw new IllegalArgumentException("不支持的文件格式，仅支持.xlsx和.xls");
            }
            
            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            
            // 获取表头（第一行）
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel文件格式错误：没有表头行");
            }
            
            // 处理数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell domainCell = row.getCell(0);
                if (domainCell == null || domainCell.getCellType() == CellType.BLANK) {
                    continue; // 跳过没有域名的行
                }
                
                String domain = getCellValueAsString(domainCell);
                if (domain.trim().isEmpty()) {
                    continue;
                }
                
                DomainConfig config = new DomainConfig();
                config.setDomain(domain);
                
                // 标题
                Cell titleCell = row.getCell(1);
                if (titleCell != null) {
                    config.setTitle(getCellValueAsString(titleCell));
                }
                
                // 描述
                Cell descCell = row.getCell(2);
                if (descCell != null) {
                    config.setDescription(getCellValueAsString(descCell));
                }
                
                // 关键词
                Cell keywordsCell = row.getCell(3);
                if (keywordsCell != null) {
                    config.setKeywords(getCellValueAsString(keywordsCell));
                }
                
                // 模板路径
                Cell templateCell = row.getCell(4);
                if (templateCell != null) {
                    config.setViewsPath(getCellValueAsString(templateCell));
                }
                
                // 状态
                Cell statusCell = row.getCell(5);
                if (statusCell != null) {
                    String statusStr = getCellValueAsString(statusCell);
                    if ("启用".equals(statusStr) || "1".equals(statusStr)) {
                        config.setStatus(1);
                    } else {
                        config.setStatus(0);
                    }
                } else {
                    config.setStatus(1); // 默认启用
                }
                
                // 每日文章数量
                Cell dailyNewsCell = row.getCell(6);
                if (dailyNewsCell != null) {
                    try {
                        if (dailyNewsCell.getCellType() == CellType.NUMERIC) {
                            config.setDailyAddNewsCount((int) dailyNewsCell.getNumericCellValue());
                        } else {
                            String value = getCellValueAsString(dailyNewsCell);
                            if (!value.isEmpty()) {
                                config.setDailyAddNewsCount(Integer.parseInt(value));
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("每日文章数量格式错误，使用默认值: {}", e.getMessage());
                        config.setDailyAddNewsCount(50); // 默认值
                    }
                }
                
                // 添加到列表
                domainConfigs.add(config);
            }
            
            return domainConfigs;
            
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("关闭Workbook失败", e);
                }
            }
        }
    }
    
    /**
     * 从单元格获取字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // 避免数值显示为科学计数法
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }
    
    /**
     * 下载Excel模板文件
     */
    @GetMapping("/domain/excel-template")
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            Resource resource = resourceLoader.getResource("classpath:static/批量模版表格.xlsx");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"domain_template.xlsx\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("下载Excel模板文件失败", e);
            return ResponseEntity.notFound().build();
        }
    }
} 
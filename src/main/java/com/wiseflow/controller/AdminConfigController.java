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
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;

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
     * 获取所有手机号分组信息
     * 返回每个手机号分组的汇总信息（手机号、总配置数、启用配置数）
     */
    @GetMapping("/domain/phone-groups")
    public ResponseEntity<List<DomainConfig>> getPhoneGroups() {
        log.info("获取所有域名配置数据");
        
        // 直接返回所有配置数据，让前端进行分组处理
        List<DomainConfig> allConfigs = domainConfigService.findAll();
        
        log.info("返回{}个域名配置", allConfigs.size());
        return ResponseEntity.ok(allConfigs);
    }
    
    /**
     * 根据服务器编号获取批次信息
     */
    @GetMapping("/domain/by-server")
    public ResponseEntity<List<Map<String, Object>>> getBatchesByServer(@RequestParam String serverInfo) {
        log.info("根据服务器编号获取批次信息: serverInfo={}", serverInfo);
        
        // 查询该服务器下的所有配置
        List<DomainConfig> allConfigs = domainConfigService.findAll().stream()
                .filter(config -> {
                    String configServerInfo = Optional.ofNullable(config.getServerInfo()).orElse("未设置服务器");
                    return configServerInfo.equals(serverInfo);
                })
                .collect(Collectors.toList());
        
        // 按手机号分组
        Map<String, List<DomainConfig>> groupsByPhone = allConfigs.stream()
                .collect(Collectors.groupingBy(
                        config -> Optional.ofNullable(config.getContactPhone()).orElse("未设置"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        
        // 构建批次信息列表
        List<Map<String, Object>> batches = groupsByPhone.entrySet().stream()
                .map(entry -> {
                    String phone = entry.getKey();
                    List<DomainConfig> configs = entry.getValue();
                    int totalCount = configs.size();
                    int activeCount = (int) configs.stream().filter(c -> c.getStatus() == 1).count();
                    
                    Map<String, Object> batchInfo = new HashMap<>();
                    batchInfo.put("phone", phone);
                    batchInfo.put("serverInfo", serverInfo);
                    batchInfo.put("domainCount", totalCount);
                    batchInfo.put("enabledCount", activeCount);
                    batchInfo.put("disabledCount", totalCount - activeCount);
                    batchInfo.put("contactPhone", phone); // 保持与DomainConfig属性命名一致
                    return batchInfo;
                })
                .sorted(Comparator.comparing(m -> ((String) m.get("phone"))))
                .collect(Collectors.toList());

        log.info("找到{}个批次", batches.size());
        return ResponseEntity.ok(batches);
    }
    
    /**
     * 根据手机号获取域名配置列表
     */
    @GetMapping("/domain/by-phone")
    public ResponseEntity<List<DomainConfig>> getDomainConfigsByPhone(@RequestParam String phone, @RequestParam String domain) {
        log.info("根据手机号获取域名配置: phone={}", phone);
        
        // 查询所有配置
        List<DomainConfig> allConfigs = domainConfigService.findAllPhone(phone, domain);

        log.info("找到{}个域名配置", allConfigs.size());
        return ResponseEntity.ok(allConfigs);
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
        
        int importedCount = domainConfigService.batchImport(domainConfigs, true);
        
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
            int importedCount = domainConfigService.batchImport(domainConfigs, true);
            
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
        
        // 生成随机批次号（用于标识同一批次导入的所有域名）
        String batchPhone = generateRandomPhone();
        log.info("生成批次号：{}", batchPhone);
        
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
                
                // 主IP
                Cell ipCell = row.getCell(1);
                if (ipCell != null) {
                   // config.setMainIp(getCellValueAsString(ipCell));
                }
                
                // 标题
                Cell titleCell = row.getCell(2);
                if (titleCell != null) {
                    config.setTitle(getCellValueAsString(titleCell));
                }
                
                // 关键词
                Cell keywordsCell = row.getCell(3);
                if (keywordsCell != null) {
                    config.setKeywords(getCellValueAsString(keywordsCell));
                }
                
                // 描述
                Cell descCell = row.getCell(4);
                if (descCell != null) {
                    config.setDescription(getCellValueAsString(descCell));
                }
                
                // 备注/服务器编号
                Cell remarkCell = row.getCell(5);
                if (remarkCell != null) {
                    config.setServerInfo(getCellValueAsString(remarkCell));
                }
                
                // 模板路径
                Cell templateCell = row.getCell(6);
                if (templateCell != null) {
                    config.setViewsPath(getCellValueAsString(templateCell));
                }
                
                // 评论规则IDs
                Cell commentRuleCell = row.getCell(7);
                if (commentRuleCell != null) {
                    String commentRules = getCellValueAsString(commentRuleCell);
                    if (!commentRules.isEmpty()) {
                        config.setCommentRuleIds(Arrays.asList(commentRules.split(","))
                            .stream()
                            .map(s -> s.trim())
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
                    }
                }
                
                // 改写规则IDs
                Cell rewriteRuleCell = row.getCell(8);
                if (rewriteRuleCell != null) {
                    String rewriteRules = getCellValueAsString(rewriteRuleCell);
                    if (!rewriteRules.isEmpty()) {
                        config.setRewriteRuleIds(Arrays.asList(rewriteRules.split(","))
                            .stream()
                            .map(s -> s.trim())
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
                    }
                }
                
                // 文章分配规则ID
                Cell articleRuleCell = row.getCell(9);
                if (articleRuleCell != null) {
                    String articleRule = getCellValueAsString(articleRuleCell);
                    if (!articleRule.isEmpty()) {
                        config.setArticleRuleIds(Arrays.asList(articleRule.split(","))
                            .stream()
                            .map(s -> s.trim())
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
                    }
                }
                
                // 设置默认状态为启用
                config.setStatus(1);
                
                // 设置默认的每日文章数量
                config.setDailyAddNewsCount(50);
                
                // 设置批次号作为手机号
                config.setContactPhone(batchPhone);
                
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

    /**
     * 批量删除域名配置
     */
    @PostMapping("/domain/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDeleteDomainConfigs(@RequestBody List<Integer> ids) {
        log.info("批量删除域名配置: count={}", ids.size());
        
        int deletedCount = 0;
        for (Integer id : ids) {
            try {
                domainConfigService.deleteById(id);
                deletedCount++;
            } catch (Exception e) {
                log.error("删除域名配置失败: id={}", id, e);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "成功删除 " + deletedCount + " 条配置");
        response.put("deletedCount", deletedCount);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 批量删除指定手机号下的域名配置
     * 删除逻辑：
     * 1. 获取到这个批次的所有域名
     * 2. 根据域名查询域名配置【关键词，评论，文章】并删除配置内容
     * 3. 删除域名配置
     * 4. 删除文章和域名的绑定关系
     * 5. 删除域名
     */
    @PostMapping("/domain/batch-delete-by-phone")
    public ResponseEntity<Map<String, Object>> batchDeleteByPhone(@RequestBody BatchDeleteRequest request) {
        log.info("批量删除指定手机号下的域名配置: phone={}, domainIds={}", 
                request.getPhone(), request.getDomainIds());
        
        try {
            // 调用Service层执行批量删除
            DomainConfigService.BatchDeleteResult result = domainConfigService.batchDeleteByPhone(
                request.getPhone(), 
                request.getDomainIds()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("deletedCount", result.getDeletedCount());
            response.put("deletedDomains", result.getDeletedDomains());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量删除失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "批量删除失败: " + e.getMessage());
            response.put("deletedCount", 0);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 批量删除请求参数
     */
    public static class BatchDeleteRequest {
        private String phone;
        private List<Integer> domainIds;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public List<Integer> getDomainIds() {
            return domainIds;
        }

        public void setDomainIds(List<Integer> domainIds) {
            this.domainIds = domainIds;
        }
    }

    /// keyword/save
    /// comment/save
    /// article/save
    /// /**
    ///   const data = {
    /// domainIds: formData.get('domainIds'),
    /// phone: formData.get('phone'),
    /// selectedRules: selectedRules,
    /// overwriteExisting: formData.has('overwriteExisting')
    /// };
    @PostMapping("/article/save")
    public ResponseEntity<Map<String, Object>> saveArticleConfig(@RequestBody Map<String, Object> data) {
        log.info("保存文章配置: {}", data);

        // 从JSON字符串中提取domainIds数组
        String domainIdsJson = (String) data.get("domainIds");
        // 去掉方括号并分割成数组
        String[] domainIdStrArray = domainIdsJson.substring(1, domainIdsJson.length()-1).split(",\\s*");
        List<Integer> domainIds = Arrays.stream(domainIdStrArray)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        String phone = (String) data.get("phone");
        List<Integer> selectedRules = (List<Integer>) data.get("selectedRules");
        boolean overwriteExisting = (boolean) data.get("overwriteExisting");
        // 保存文章配置
        domainConfigService.saveArticleConfig(domainIds, phone, selectedRules, overwriteExisting);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/keyword/save")
    public ResponseEntity<Map<String, Object>> saveKeywordConfig(@RequestBody Map<String, Object> data) {
        log.info("保存关键词配置: {}", data);

        // 从JSON字符串中提取domainIds数组
        String domainIdsJson = (String) data.get("domainIds");
        // 去掉方括号并分割成数组
        String[] domainIdStrArray = domainIdsJson.substring(1, domainIdsJson.length()-1).split(",\\s*");
        List<Integer> domainIds = Arrays.stream(domainIdStrArray)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        String phone = (String) data.get("phone");
        List<Integer> selectedRules = (List<Integer>) data.get("selectedRules");
        boolean overwriteExisting = (boolean) data.get("overwriteExisting");
        // 保存关键词配置
        domainConfigService.saveKeywordConfig(domainIds, phone, selectedRules, overwriteExisting);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/comment/save")
    public ResponseEntity<Map<String, Object>> saveCommentConfig(@RequestBody Map<String, Object> data) {
        log.info("保存评论配置: {}", data);
        
        // 从JSON字符串中提取domainIds数组
        String domainIdsJson = (String) data.get("domainIds");
        // 去掉方括号并分割成数组
        String[] domainIdStrArray = domainIdsJson.substring(1, domainIdsJson.length()-1).split(",\\s*");
        List<Integer> domainIds = Arrays.stream(domainIdStrArray)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            
        String phone = (String) data.get("phone");
        List<Integer> selectedRules = (List<Integer>) data.get("selectedRules");
        boolean overwriteExisting = (boolean) data.get("overwriteExisting");
        
        // 保存评论配置
        domainConfigService.saveCommentConfig(domainIds, phone, selectedRules, overwriteExisting);
        return ResponseEntity.ok(data);
    }

    /**
     * 检查域名是否存在
     * 批量检查多个域名，返回已存在的域名列表
     */
    @PostMapping("/domain/check-exists")
    public ResponseEntity<List<String>> checkDomainsExist(@RequestBody List<String> domains) {
        log.info("批量检查域名是否存在: domains={}", domains);
        
        if (domains == null || domains.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        // 过滤出已存在的域名
        List<String> existingDomains = domains.stream()
                .filter(domain -> domainConfigService.isDomainExists(domain))
                .collect(Collectors.toList());
        
        log.info("检查结果: 总数={}, 已存在={}", domains.size(), existingDomains.size());
        return ResponseEntity.ok(existingDomains);
    }
    
    /**
     * 单条导入域名配置
     * 支持覆盖已存在的配置
     */
    @PostMapping("/domain/single")
    public ResponseEntity<Map<String, Object>> importSingleDomain(
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取请求参数
            @SuppressWarnings("unchecked")
            Map<String, Object> domainConfigMap = (Map<String, Object>) request.get("domainConfig");
            Boolean overwrite = (Boolean) request.getOrDefault("overwrite", false);
            
            if (domainConfigMap == null) {
                throw new IllegalArgumentException("缺少必要的domainConfig参数");
            }
            
            String domain = (String) domainConfigMap.get("domain");
            if (domain == null || domain.trim().isEmpty()) {
                throw new IllegalArgumentException("域名不能为空");
            }
            
            log.info("单条导入域名配置: domain={}, overwrite={}", domain, overwrite);
            
            // 检查域名是否已存在
            boolean exists = domainConfigService.isDomainExists(domain);
            
            // 如果域名已存在且不允许覆盖，返回错误
            if (exists && !overwrite) {
                response.put("success", false);
                response.put("message", "域名已存在，未设置覆盖");
                return ResponseEntity.ok(response);
            }
            
            // 创建域名配置对象
            DomainConfig domainConfig = new DomainConfig();
            domainConfig.setDomain(domain);
            domainConfig.setTitle((String) domainConfigMap.getOrDefault("title", domain));
            domainConfig.setDescription((String) domainConfigMap.getOrDefault("description", ""));
            domainConfig.setKeywords((String) domainConfigMap.getOrDefault("keywords", ""));
            domainConfig.setServerInfo((String) domainConfigMap.getOrDefault("serverInfo", ""));
            
            // 使用前端传递的批次号（手机号）
            String contactPhone = (String) domainConfigMap.get("contactPhone");
            if (contactPhone == null || contactPhone.trim().isEmpty()) {
                // 如果前端未传递批次号，才使用随机生成的值
                contactPhone = generateRandomPhone();
            }
            domainConfig.setContactPhone(contactPhone);
            
            // 设置模版路径
            String viewsPath = (String) domainConfigMap.get("viewsPath");
            if (viewsPath != null && !viewsPath.trim().isEmpty()) {
                domainConfig.setViewsPath(viewsPath);
            }
            
            // 设置评论规则IDs
            @SuppressWarnings("unchecked")
            List<Integer> commentRuleIds = (List<Integer>) domainConfigMap.get("commentRuleIds");
            if (commentRuleIds != null && !commentRuleIds.isEmpty()) {
                domainConfig.setCommentRuleIds(commentRuleIds);
            }
            
            // 设置改写规则IDs
            @SuppressWarnings("unchecked")
            List<Integer> rewriteRuleIds = (List<Integer>) domainConfigMap.get("rewriteRuleIds");
            if (rewriteRuleIds != null && !rewriteRuleIds.isEmpty()) {
                domainConfig.setRewriteRuleIds(rewriteRuleIds);
            }
            
            // 设置文章分配规则IDs
            @SuppressWarnings("unchecked")
            List<Integer> articleRuleIds = (List<Integer>) domainConfigMap.get("articleRuleIds");
            if (articleRuleIds != null && !articleRuleIds.isEmpty()) {
                domainConfig.setArticleRuleIds(articleRuleIds);
            }
            
            // 设置状态
            Integer status = domainConfigMap.get("status") instanceof Number ? 
                    ((Number) domainConfigMap.get("status")).intValue() : 1;
            domainConfig.setStatus(status);
            
            // 设置默认的每日文章数量
            domainConfig.setDailyAddNewsCount(50);
            
            // 设置默认Logo路径
            domainConfig.setLogoUrl("/assets/images/logo.png");
            
            // 设置默认友情链接
            domainConfig.setFriendlyLinks("[]");
            
            // 如果域名已存在且允许覆盖，先删除旧的配置
            if (exists && overwrite) {
                // 先获取已有配置的ID
                Optional<DomainConfig> existingConfig = domainConfigService.findByDomain(domain);
                if (existingConfig.isPresent()) {
                    domainConfigService.deleteById(existingConfig.get().getId());
                }
            }

            List<DomainConfig> domainConfigs = new ArrayList<>();
            domainConfigs.add(domainConfig);
            // 保存新的配置
            domainConfigService.batchImport(domainConfigs, overwrite);
            
            response.put("success", true);
            response.put("message", exists ? "域名配置已更新" : "域名配置已创建");
            response.put("data", domainConfig);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("单条导入域名配置失败", e);
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 生成随机手机号（批次号）
     */
    private String generateRandomPhone() {
        // 生成随机手机号
        StringBuilder phoneBuilder = new StringBuilder("1");
        
        // 第二位数字：3, 4, 5, 6, 7, 8, 9
        String[] secondDigits = {"3", "4", "5", "6", "7", "8", "9"};
        phoneBuilder.append(secondDigits[(int) (Math.random() * secondDigits.length)]);
        
        // 生成剩余9位数字
        for (int i = 0; i < 9; i++) {
            phoneBuilder.append((int) (Math.random() * 10));
        }
        
        return phoneBuilder.toString();
    }

    
    
     





} 
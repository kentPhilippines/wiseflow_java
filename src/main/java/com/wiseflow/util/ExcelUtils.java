package com.wiseflow.util;

import com.wiseflow.entity.DomainConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel工具类
 * 用于处理导入导出Excel文件
 */
@Slf4j
public class ExcelUtils {

    /**
     * 解析Excel模板文件为DomainConfig对象列表
     *
     * @param file Excel文件
     * @return 域名配置列表
     */
    public static List<DomainConfig> parseDomainConfigExcel(MultipartFile file) throws IOException {
        List<DomainConfig> configList = new ArrayList<>();
        
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            
            // 获取表头
            Row headerRow = sheet.getRow(0);
            Map<Integer, String> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    headerMap.put(i, cell.getStringCellValue().trim());
                }
            }
            
            // 从第二行开始解析数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // 如果域名列为空，跳过该行
                Cell domainCell = row.getCell(getColumnIndex(headerMap, "域名"));
                if (domainCell == null || domainCell.getStringCellValue().trim().isEmpty()) {
                    continue;
                }
                
                DomainConfig config = new DomainConfig();
                
                // 设置基本属性
                config.setDomain(getCellValueAsString(row, getColumnIndex(headerMap, "域名")));
                config.setTitle(getCellValueAsString(row, getColumnIndex(headerMap, "标题")));
                config.setDescription(getCellValueAsString(row, getColumnIndex(headerMap, "描述")));
                config.setKeywords(getCellValueAsString(row, getColumnIndex(headerMap, "关键词")));
                
                // 设置可选属性
                int statusIndex = getColumnIndex(headerMap, "状态");
                if (statusIndex >= 0) {
                    String statusValue = getCellValueAsString(row, statusIndex);
                    config.setStatus("启用".equals(statusValue) ? 1 : 0);
                } else {
                    config.setStatus(1); // 默认启用
                }
                
                int templateIndex = getColumnIndex(headerMap, "模板路径");
                if (templateIndex >= 0) {
                    config.setViewsPath(getCellValueAsString(row, templateIndex));
                } else {
                    config.setViewsPath("/templates/default"); // 默认模板路径
                }
                
                int logoIndex = getColumnIndex(headerMap, "Logo URL");
                if (logoIndex >= 0) {
                    config.setLogoUrl(getCellValueAsString(row, logoIndex));
                }
                
                int faviconIndex = getColumnIndex(headerMap, "Favicon URL");
                if (faviconIndex >= 0) {
                    config.setFaviconUrl(getCellValueAsString(row, faviconIndex));
                }
                
                int copyrightIndex = getColumnIndex(headerMap, "版权信息");
                if (copyrightIndex >= 0) {
                    config.setCopyright(getCellValueAsString(row, copyrightIndex));
                }
                
                int icpIndex = getColumnIndex(headerMap, "备案号");
                if (icpIndex >= 0) {
                    config.setIcp(getCellValueAsString(row, icpIndex));
                }
                
                int emailIndex = getColumnIndex(headerMap, "联系邮箱");
                if (emailIndex >= 0) {
                    config.setContactEmail(getCellValueAsString(row, emailIndex));
                }
                
                int phoneIndex = getColumnIndex(headerMap, "联系电话");
                if (phoneIndex >= 0) {
                    config.setContactPhone(getCellValueAsString(row, phoneIndex));
                }
                
                int addressIndex = getColumnIndex(headerMap, "联系地址");
                if (addressIndex >= 0) {
                    config.setContactAddress(getCellValueAsString(row, addressIndex));
                }
                
                int dailyNewsIndex = getColumnIndex(headerMap, "每日新增文章数");
                if (dailyNewsIndex >= 0) {
                    String dailyNews = getCellValueAsString(row, dailyNewsIndex);
                    if (!dailyNews.isEmpty()) {
                        try {
                            config.setDailyAddNewsCount(Integer.parseInt(dailyNews));
                        } catch (NumberFormatException e) {
                            config.setDailyAddNewsCount(50); // 默认50
                        }
                    } else {
                        config.setDailyAddNewsCount(50); // 默认50
                    }
                } else {
                    config.setDailyAddNewsCount(50); // 默认50
                }
                
                // 设置创建时间和更新时间
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                
                // 设置友情链接为空数组
                config.setFriendlyLinks("[]");
                
                configList.add(config);
            }
            
            workbook.close();
        }
        
        return configList;
    }
    
    /**
     * 获取列索引
     */
    private static int getColumnIndex(Map<Integer, String> headerMap, String columnName) {
        for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
            if (columnName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return -1;
    }
    
    /**
     * 获取单元格的字符串值
     */
    private static String getCellValueAsString(Row row, int columnIndex) {
        if (columnIndex < 0) return "";
        
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // 避免科学计数法显示
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
} 
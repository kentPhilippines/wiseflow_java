package com.wiseflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ServiceException;
import com.wiseflow.entity.*;
import com.wiseflow.mapper.*;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Set;
import com.wiseflow.dto.DomainConfigRequest;

import javax.annotation.Resource;

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
    private final JdbcTemplate jdbcTemplate;
    
    // 保存的模板配置
    private static DomainConfig templateConfig;
    
    // 默认模板路径
    @Value("${app.default-template-path:/templates/default}")
    private String defaultTemplatePath;

    @Resource
    private DomainSeoKeywordMapper DomainSeoKeywordMapper;
    @Resource
    private SeoKeywordMapper seoKeywordMapper;



    @Resource
    private CommentRuleMapper commentRuleMapper;

    @Resource
    private DomainCommentRuleMapper   DomainCommentRuleMapper;

    @Resource
    private DomainArticleConfigMapper articleConfigMapper;

    @Resource
    private ArticleRuleMapper articleRuleMapper;





    @Autowired
    private NewsMapper newsMapper;

    @Override
    public Long count() {
        return domainConfigMapper.selectCount(null);
    }

    @Override
    public Long countEnabled() {
        LambdaQueryWrapper<DomainConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DomainConfig::getStatus, 1);
        return domainConfigMapper.selectCount(queryWrapper);
    }

    @Override
    public DomainConfig save(DomainConfig domainConfig) {
        // 自动生成其他字段
        autoGenerateFields(domainConfig);
        
        // 处理友情链接
        processFriendlyLinks(domainConfig);
        
        if (domainConfig.getId() == null) {
            domainConfig.setCreateTime(LocalDateTime.now());
            domainConfig.setUpdateTime(LocalDateTime.now());
            domainConfigMapper.insert(domainConfig);
        } else {
            domainConfig.setUpdateTime(LocalDateTime.now());
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
    public void toggleStatus(Integer id, Integer status) {
        findById(id).orElseThrow(() -> new NoSuchElementException("Domain config not found with id: " + id));
        log.info("更新域名配置状态: id={}, status={}", id, status);
        DomainConfig newdomainConfig = new DomainConfig();
        newdomainConfig.setId(id);
        newdomainConfig.setStatus(status);
        newdomainConfig.setUpdateTime(LocalDateTime.now());
        domainConfigMapper.updateById(newdomainConfig);
    }
    
    @Override
    public List<DomainConfig> findAllEnabled() {
        LambdaQueryWrapper<DomainConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DomainConfig::getStatus, 1);
        List<DomainConfig> domainConfigs = domainConfigMapper.selectList(queryWrapper);
        // 解析所有配置的友情链接
        domainConfigs.forEach(this::parseFriendlyLinks);
        return domainConfigs;
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
    
    @Override
    @Transactional
    public int batchImport(List<DomainConfig> domainConfigs, boolean overwrite) {
        if (domainConfigs == null || domainConfigs.isEmpty()) {
            return 0;
        }
        
        int importedCount = 0;
        
        // 优化：使用IN查询一次性获取所有已存在的域名
        List<String> domains = domainConfigs.stream()
                .map(DomainConfig::getDomain)
                .collect(Collectors.toList());
        
        // 查找所有已存在的域名
        LambdaQueryWrapper<DomainConfig> existingDomainsQuery = new LambdaQueryWrapper<>();
        existingDomainsQuery.in(DomainConfig::getDomain, domains);
        List<DomainConfig> existingConfigs = domainConfigMapper.selectList(existingDomainsQuery);
        
        // 提取已存在的域名集合，用于快速查找
        Set<String> existingDomains = existingConfigs.stream()
                .map(DomainConfig::getDomain)
                .collect(Collectors.toSet());
        
        // 处理每个导入的配置
        List<DomainConfig> configsToInsert = new ArrayList<>();
        
        for (DomainConfig config : domainConfigs) {
            // 处理友情链接, 转为JSON字符串
            try {
                if (config.getFriendlyLinkList() != null && !config.getFriendlyLinkList().isEmpty()) {
                    config.setFriendlyLinks(objectMapper.writeValueAsString(config.getFriendlyLinkList()));
                }
            } catch (JsonProcessingException e) {
                log.error("处理友情链接时发生错误: {}", e.getMessage(), e);
                config.setFriendlyLinks("[]");
            }
            
            // 补充缺失的字段
            if (config.getStatus() == null) {
                config.setStatus(1); // 默认启用
            }
            if (config.getViewsPath() == null || config.getViewsPath().isEmpty()) {
                config.setViewsPath(defaultTemplatePath);
            }
            
            boolean exists = existingDomains.contains(config.getDomain());
            
            if (!exists || overwrite) {
                configsToInsert.add(config);
                importedCount++;
            }
        }
        
        if (configsToInsert.isEmpty()) {
            return 0;
        }
        
        // 如果覆盖，先删除已存在的记录
        if (overwrite) {
            List<String> domainsToOverwrite = configsToInsert.stream()
                    .map(DomainConfig::getDomain)
                    .filter(existingDomains::contains)
                    .collect(Collectors.toList());
            
            if (!domainsToOverwrite.isEmpty()) {
                LambdaQueryWrapper<DomainConfig> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.in(DomainConfig::getDomain, domainsToOverwrite);
                domainConfigMapper.delete(deleteWrapper);
            }
        }
        
        // 批量插入
        for (DomainConfig config : configsToInsert) {
            // 清除ID，确保新增
            config.setId(null);
            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            config.setCreateTime(now);
            config.setUpdateTime(now);
            // 插入记录
            domainConfigMapper.insert(config);
        }
        
        return importedCount;
    }
    
    @Override
    public void saveTemplate(DomainConfig template) {
        // 简单实现: 保存到静态变量
        // 实际生产中应该保存到数据库或配置文件
        templateConfig = template;
    }
    
    // 获取当前模板
    public static DomainConfig getTemplate() {
        return templateConfig;
    }

    /**
     * 自动生成其他字段
     */
    private void autoGenerateFields(DomainConfig domainConfig) {
        // 如果没有设置状态，默认为启用
        if (domainConfig.getStatus() == null) {
            domainConfig.setStatus(1);
        }

        // 如果没有设置模板路径，使用默认模板
        if (domainConfig.getViewsPath() == null || domainConfig.getViewsPath().isEmpty()) {
            domainConfig.setViewsPath(defaultTemplatePath);
        }

        // 如果没有设置每日文章数量，默认为50
        if (domainConfig.getDailyAddNewsCount() == null) {
            domainConfig.setDailyAddNewsCount(50);
        }

        // 如果没有设置Logo URL，根据域名生成默认Logo
        if (domainConfig.getLogoUrl() == null || domainConfig.getLogoUrl().isEmpty()) {
            domainConfig.setLogoUrl("https://" + domainConfig.getDomain() + "/logo.png");
        }

        // 如果没有设置ICP备案号，自动生成
        if (domainConfig.getIcp() == null || domainConfig.getIcp().isEmpty()) {
            domainConfig.setIcp("ICP备" + generateRandomNumber(8) + "号");
        }

        // 如果没有设置友情链接，初始化为空列表
        if (domainConfig.getFriendlyLinks() == null) {
            domainConfig.setFriendlyLinks("[]");
        }

        // 如果没有设置版权信息，根据域名生成
        if (domainConfig.getCopyright() == null || domainConfig.getCopyright().isEmpty()) {
            domainConfig.setCopyright("© " + LocalDateTime.now().getYear() + " " + domainConfig.getDomain() + " All Rights Reserved.");
        }
    }

    /**
     * 生成指定长度的随机数字
     */
    private String generateRandomNumber(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveKeywordConfig(Long domainId, DomainConfigRequest request) {
        // 1. 删除原有配置
        DomainSeoKeywordMapper.delete(new LambdaQueryWrapper<DomainSeoKeyword>()
                .eq(DomainSeoKeyword::getDomainConfigId, domainId));

        // 2. 保存新配置
        if (request.getConfigIds() != null && !request.getConfigIds().isEmpty()) {
            request.getConfigIds().forEach(keywordId -> {
                DomainSeoKeyword config = new DomainSeoKeyword();
                config.setDomainConfigId(domainId.intValue());
                config.setSeoKeywordId(keywordId.intValue());
                DomainSeoKeywordMapper.insert(config);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCommentConfig(Long domainId, DomainConfigRequest request) {
        // 1. 删除原有配置
        DomainCommentRuleMapper.delete(new LambdaQueryWrapper<DomainCommentRule>()
                .eq(DomainCommentRule::getDomainConfigId, domainId));
        
        // 2. 保存新配置
        if (request.getConfigIds() != null && !request.getConfigIds().isEmpty()) {
            request.getConfigIds().forEach(ruleId -> {
                DomainCommentRule config = new DomainCommentRule();
                config.setDomainConfigId(domainId.intValue());
                config.setCommentRuleId(ruleId.intValue());
                DomainCommentRuleMapper.insert(config);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveArticleConfig(Long domainId, DomainConfigRequest request) {
        // 1. 删除原有配置
        articleConfigMapper.delete(new LambdaQueryWrapper<DomainArticleConfig>()
                .eq(DomainArticleConfig::getDomainId, domainId));
        
        // 2. 保存新配置
        if (request.getConfigIds() != null && !request.getConfigIds().isEmpty()) {
            int sort = 0;
            for (Long typeId : request.getConfigIds()) {
                DomainArticleConfig config = new DomainArticleConfig();
                config.setDomainId(domainId);
                config.setTypeId(typeId);
                config.setSort(sort++);
                articleConfigMapper.insert(config);
            }
        }
    }






    @Override
    public List<SeoKeyword> getAllDomainKeywords(DomainConfig domainConfig) {
        // 1. 获取域名ID
        Integer domainId = domainConfig.getId();

        // 2. 查询该域名下的所有SEO关键词配置
        List<DomainSeoKeyword> configs = DomainSeoKeywordMapper.selectList(
            new LambdaQueryWrapper<DomainSeoKeyword>()
                .eq(DomainSeoKeyword::getDomainConfigId, domainId)
                .orderByAsc(DomainSeoKeyword::getCreateTime)
        );

        // 3. 如果没有配置规则,返回空列表
        if(configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 获取所有规则ID
        List<Integer> collect = configs.stream()
                .map(DomainSeoKeyword::getSeoKeywordId)
                .collect(Collectors.toList());

        // 5. 查询并返回完整的规则信息
        return seoKeywordMapper.selectBatchIds(collect);
    }

    @Override
    public List<CommentRule> getAllDomainCommentRules(DomainConfig domainConfig) {
        // 1. 获取域名ID
        Integer domainId = domainConfig.getId();

        // 2. 查询该域名下的所有评论规则配置
        List<DomainCommentRule> configs = DomainCommentRuleMapper.selectList(
            new LambdaQueryWrapper<DomainCommentRule>()
                .eq(DomainCommentRule::getDomainConfigId, domainId)
                .orderByAsc(DomainCommentRule::getCreateTime)
        );

        // 3. 如果没有配置规则,返回空列表
        if(configs == null || configs.isEmpty()) {
            return new ArrayList<>();       
        }

        // 4. 获取所有规则ID
        List<Integer> collect = configs.stream()
                .map(DomainCommentRule::getCommentRuleId)
                .collect(Collectors.toList());
        List<CommentRule> commentRules = commentRuleMapper.selectBatchIds(collect);

        return commentRules;
    }

    @Override
    public List<ArticleRule> getAllDomainArticleConfigs(DomainConfig domainConfig) {
        // 1. 获取域名ID
        Integer domainId = domainConfig.getId();

        // 2. 查询该域名下的所有文章配置规则
        List<DomainArticleConfig> configs = articleConfigMapper.selectList(
            new LambdaQueryWrapper<DomainArticleConfig>()
                .eq(DomainArticleConfig::getDomainId, domainId)
                .orderByAsc(DomainArticleConfig::getSort)
        );
        
        // 3. 如果没有配置规则,返回空列表
        if(configs == null || configs.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 4. 获取所有规则ID
        List<Long> ruleIds = configs.stream()
            .map(DomainArticleConfig::getTypeId)
            .collect(Collectors.toList());
            
        // 5. 查询并返回完整的规则信息
        return articleRuleMapper.selectBatchIds(ruleIds);
    }

    @Override
    public List<DomainConfig> getEnabledDomainConfigs() {
        log.info("获取所有启用的域名配置");
        
        LambdaQueryWrapper<DomainConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DomainConfig::getStatus, 1) // 状态为1表示启用
                .orderByDesc(DomainConfig::getUpdateTime);
        
        List<DomainConfig> domainConfigs = domainConfigMapper.selectList(queryWrapper);
        log.info("找到{}个启用的域名配置", domainConfigs.size());
        
        return domainConfigs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchGenerate(List<String> domains, Long templateId) {
        return 1;
    }
} 
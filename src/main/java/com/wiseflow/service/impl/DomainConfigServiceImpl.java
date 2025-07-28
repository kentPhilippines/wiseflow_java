package com.wiseflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

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
import java.util.Random;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.wiseflow.dto.DomainConfigRequest;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;

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

    private final DomainSeoKeywordMapper DomainSeoKeywordMapper;
    private final SeoKeywordMapper seoKeywordMapper;

    private final CommentRuleMapper commentRuleMapper;

    private final DomainCommentRuleMapper DomainCommentRuleMapper;

    private final DomainArticleConfigMapper articleConfigMapper;

    private final ArticleRuleMapper articleRuleMapper;

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
        LambdaQueryWrapper<DomainConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.groupBy(DomainConfig::getContactPhone);
        List<DomainConfig> domainConfigs = domainConfigMapper.selectList(queryWrapper);
        
        // 统计总域名数、开启和关闭数
        for (DomainConfig config : domainConfigs) {
            LambdaQueryWrapper<DomainConfig> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(DomainConfig::getContactPhone, config.getContactPhone());
            
            // 总域名数
            config.setDomainCount(domainConfigMapper.selectCount(countWrapper).intValue());
            
            // 开启数量
            countWrapper.clear();
            countWrapper.eq(DomainConfig::getContactPhone, config.getContactPhone())
                       .eq(DomainConfig::getStatus, 1);
            config.setEnabledCount(domainConfigMapper.selectCount(countWrapper).intValue());
            
            // 关闭数量
            countWrapper.clear();
            countWrapper.eq(DomainConfig::getContactPhone, config.getContactPhone())
                       .eq(DomainConfig::getStatus, 0);
            config.setDisabledCount(domainConfigMapper.selectCount(countWrapper).intValue());
        }

        // 解析所有配置的友情链接
        domainConfigs.forEach(this::parseFriendlyLinks);
        return domainConfigs;
    }

    @Override
    public DomainConfig update(DomainConfig domainConfig) {
        // 确保配置存在
        findById(domainConfig.getId()).orElseThrow(
                () -> new NoSuchElementException("Domain config not found with id: " + domainConfig.getId()));

        // 处理友情链接
        processFriendlyLinks(domainConfig);

        domainConfigMapper.updateById(domainConfig);
        return domainConfig;
    }

    @Override
    public void deleteById(Integer id) {
        // 首先删除关联的评论规则配置
        DomainCommentRuleMapper.delete(new LambdaQueryWrapper<DomainCommentRule>()
                .eq(DomainCommentRule::getDomainConfigId, id));

        // 删除关联的SEO关键词配置
        DomainSeoKeywordMapper.delete(new LambdaQueryWrapper<DomainSeoKeyword>()
                .eq(DomainSeoKeyword::getDomainConfigId, id));

        // 删除关联的文章分配规则配置
        articleConfigMapper.delete(new LambdaQueryWrapper<DomainArticleConfig>()
                .eq(DomainArticleConfig::getDomainId, id));

        // 最后删除域名配置本身
        domainConfigMapper.deleteById(id);

        log.info("已删除域名配置及相关联数据: id={}", id);
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
                                new TypeReference<List<DomainConfig.FriendlyLink>>() {
                                }));
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
            existingConfigs.forEach(config -> {
                //删除中间配置表
                DomainCommentRuleMapper.delete(new LambdaQueryWrapper<DomainCommentRule>()
                        .eq(DomainCommentRule::getDomainConfigId, config.getId()));
                DomainSeoKeywordMapper.delete(new LambdaQueryWrapper<DomainSeoKeyword>()
                        .eq(DomainSeoKeyword::getDomainConfigId, config.getId()));
                articleConfigMapper.delete(new LambdaQueryWrapper<DomainArticleConfig>()
                        .eq(DomainArticleConfig::getDomainId, config.getId())); 
            });
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
        //
        String icp = generateIcp();
        String contactPhone = generateContactPhone();
        
        String contactEmail = generateContactEmail();
        String contactAddress = generateContactAddress();
        // 批量插入
        for (DomainConfig config : configsToInsert) {
            // 清除ID，确保新增
            config.setId(null);
            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            config.setCreateTime(now);
            config.setUpdateTime(now);
            // 随机生成备案号
            config.setIcp(icp);
            // 随机生成版权信息
            String copyright = generateCopyright(config.getDomain());
            config.setCopyright(copyright);
            if(config.getContactPhone() == null || config.getContactPhone().isEmpty()){
                // 随机生成联系电话
                config.setContactPhone(contactPhone);
            }
            // 随机生成联系邮箱
            if(config.getContactEmail() == null || config.getContactEmail().isEmpty()){
                config.setContactEmail(contactEmail);
            }
            // 随机生成联系地址
            if(config.getContactAddress() == null || config.getContactAddress().isEmpty()){
                config.setContactAddress(contactAddress);
            }
            // 插入记录
            domainConfigMapper.insert(config);
        }
        // 插入规则中间表
        for (DomainConfig config : configsToInsert) {
            // 插入规则中间表
            if (config.getArticleRuleIds() != null && !config.getArticleRuleIds().isEmpty()) {
                config.getArticleRuleIds().forEach(ruleId -> {
                    DomainArticleConfig articleConfig = new DomainArticleConfig();
                    articleConfig.setDomainId(Long.valueOf(config.getId()));
                    articleConfig.setTypeId(Long.valueOf(ruleId));
                    articleConfig.setSort(0);
                    articleConfigMapper.insert(articleConfig);
                });
            }
            if (config.getCommentRuleIds() != null && !config.getCommentRuleIds().isEmpty()) {
                config.getCommentRuleIds().forEach(ruleId -> {
                    DomainCommentRule commentRule = new DomainCommentRule();
                    commentRule.setDomainConfigId(config.getId());
                    commentRule.setCommentRuleId(ruleId);
                    DomainCommentRuleMapper.insert(commentRule);
                });
            }
            if (config.getRewriteRuleIds() != null && !config.getRewriteRuleIds().isEmpty()) {
                config.getRewriteRuleIds().forEach(ruleId -> {
                    DomainSeoKeyword seoKeywordConfig = new DomainSeoKeyword();
                    seoKeywordConfig.setDomainConfigId(config.getId());
                    seoKeywordConfig.setSeoKeywordId(ruleId);
                    DomainSeoKeywordMapper.insert(seoKeywordConfig);
                });
            }
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
            domainConfig.setCopyright(
                    "© " + LocalDateTime.now().getYear() + " " + domainConfig.getDomain() + " All Rights Reserved.");
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
                        .orderByAsc(DomainSeoKeyword::getCreateTime));

        // 3. 如果没有配置规则,返回空列表
        if (configs == null || configs.isEmpty()) {
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
                        .orderByAsc(DomainCommentRule::getCreateTime));

        // 3. 如果没有配置规则,返回空列表
        if (configs == null || configs.isEmpty()) {
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
                        .orderByAsc(DomainArticleConfig::getSort));

        // 3. 如果没有配置规则,返回空列表
        if (configs == null || configs.isEmpty()) {
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
        if (CollUtil.isEmpty(domains)) {
            return 0;
        }

        log.info("批量生成域名配置开始，域名数量：{}，模板ID：{}", domains.size(), templateId);

        // 查询模板配置
        DomainConfig templateDomain = null;
        if (templateId != null) {
            templateDomain = domainConfigMapper.selectById(templateId);
            if (templateDomain == null) {
                return 0;
            }
        } else {
            // 如果没有指定模板ID，使用默认模板
            if (templateConfig != null) {
                templateDomain = templateConfig;
            } else {
                // 如果没有默认模板，创建一个基础模板
                templateDomain = new DomainConfig();
                templateDomain.setViewsPath(defaultTemplatePath);
                templateDomain.setDailyAddNewsCount(50);
                templateDomain.setStatus(1);
            }
        }

        log.info("使用模板：{}", templateDomain.getDomain() != null ? templateDomain.getDomain() : "默认模板");

        // 查询已存在的域名
        LambdaQueryWrapper<DomainConfig> existingQuery = new LambdaQueryWrapper<>();
        existingQuery.in(DomainConfig::getDomain, domains);
        List<DomainConfig> existingConfigs = domainConfigMapper.selectList(existingQuery);

        // 提取已存在的域名集合
        Set<String> existingDomains = existingConfigs.stream()
                .map(DomainConfig::getDomain)
                .collect(Collectors.toSet());

        int successCount = 0;
        List<DomainConfig> newConfigs = new ArrayList<>();

        // 生成随机联系信息，使所有生成的域名共享相同的联系信息
        String icp = generateIcp();
        String contactPhone = generateContactPhone();
        String contactEmail = generateContactEmail();
        String contactAddress = generateContactAddress();

        // 处理每个域名
        for (String domain : domains) {
            // 跳过已存在的域名
            if (existingDomains.contains(domain)) {
                log.info("域名已存在，跳过：{}", domain);
                continue;
            }

            // 创建新的域名配置，复制模板配置
            DomainConfig newConfig = new DomainConfig();

            // 复制模板的基本配置
            if (templateDomain.getDomain() != null) {
                BeanUtil.copyProperties(templateDomain, newConfig, "id", "domain", "createTime", "updateTime");
            }

            // 设置新域名基本信息
            newConfig.setDomain(domain);
            newConfig.setTitle(domain);
            LocalDateTime now = LocalDateTime.now();
            newConfig.setCreateTime(now);
            newConfig.setUpdateTime(now);

            // 设置站点信息
            newConfig.setIcp(icp);
            newConfig.setCopyright(generateCopyright(domain));
            newConfig.setContactPhone(contactPhone);
            newConfig.setContactEmail(contactEmail);
            newConfig.setContactAddress(contactAddress);

            // 设置默认Logo
            if (newConfig.getLogoUrl() == null || newConfig.getLogoUrl().isEmpty()) {
                newConfig.setLogoUrl("https://" + domain + "/logo.png");
            }

            // 处理友情链接
            if (templateDomain.getFriendlyLinkList() != null && !templateDomain.getFriendlyLinkList().isEmpty()) {
                newConfig.setFriendlyLinkList(new ArrayList<>(templateDomain.getFriendlyLinkList()));
                try {
                    newConfig.setFriendlyLinks(objectMapper.writeValueAsString(newConfig.getFriendlyLinkList()));
                } catch (JsonProcessingException e) {
                    log.error("处理友情链接时发生错误: {}", e.getMessage(), e);
                    newConfig.setFriendlyLinks("[]");
                }
            } else {
                newConfig.setFriendlyLinks("[]");
            }

            // 自动补充其他字段
            autoGenerateFields(newConfig);

            // 保存域名配置并将其添加到集合中
            domainConfigMapper.insert(newConfig);
            newConfigs.add(newConfig);
            successCount++;

            log.info("成功创建域名配置：{}", domain);
        }

        // 如果模板有关联的配置规则，也复制给新域名
        if (!newConfigs.isEmpty() && templateId != null) {
            // 复制SEO关键词配置
            List<DomainSeoKeyword> templateKeywords = DomainSeoKeywordMapper.selectList(
                    new LambdaQueryWrapper<DomainSeoKeyword>()
                            .eq(DomainSeoKeyword::getDomainConfigId, templateId));

            if (!templateKeywords.isEmpty()) {
                List<Integer> keywordIds = templateKeywords.stream()
                        .map(DomainSeoKeyword::getSeoKeywordId)
                        .collect(Collectors.toList());

                for (DomainConfig newConfig : newConfigs) {
                    for (Integer keywordId : keywordIds) {
                        DomainSeoKeyword newKeyword = new DomainSeoKeyword();
                        newKeyword.setDomainConfigId(newConfig.getId());
                        newKeyword.setSeoKeywordId(keywordId);
                        DomainSeoKeywordMapper.insert(newKeyword);
                    }
                }
                log.info("已复制SEO关键词配置，数量：{}", keywordIds.size());
            }

            // 复制评论规则配置
            List<DomainCommentRule> templateCommentRules = DomainCommentRuleMapper.selectList(
                    new LambdaQueryWrapper<DomainCommentRule>()
                            .eq(DomainCommentRule::getDomainConfigId, templateId));

            if (!templateCommentRules.isEmpty()) {
                List<Integer> commentRuleIds = templateCommentRules.stream()
                        .map(DomainCommentRule::getCommentRuleId)
                        .collect(Collectors.toList());

                for (DomainConfig newConfig : newConfigs) {
                    for (Integer ruleId : commentRuleIds) {
                        DomainCommentRule newRule = new DomainCommentRule();
                        newRule.setDomainConfigId(newConfig.getId());
                        newRule.setCommentRuleId(ruleId);
                        DomainCommentRuleMapper.insert(newRule);
                    }
                }
                log.info("已复制评论规则配置，数量：{}", commentRuleIds.size());
            }

            // 复制文章规则配置
            List<DomainArticleConfig> templateArticleConfigs = articleConfigMapper.selectList(
                    new LambdaQueryWrapper<DomainArticleConfig>()
                            .eq(DomainArticleConfig::getDomainId, templateId));

            if (!templateArticleConfigs.isEmpty()) {
                Map<Long, Integer> typeIdSortMap = new HashMap<>();
                templateArticleConfigs.forEach(config -> typeIdSortMap.put(config.getTypeId(), config.getSort()));

                for (DomainConfig newConfig : newConfigs) {
                    for (Map.Entry<Long, Integer> entry : typeIdSortMap.entrySet()) {
                        DomainArticleConfig newArticleConfig = new DomainArticleConfig();
                        newArticleConfig.setDomainId(Long.valueOf(newConfig.getId()));
                        newArticleConfig.setTypeId(entry.getKey());
                        newArticleConfig.setSort(entry.getValue());
                        articleConfigMapper.insert(newArticleConfig);
                    }
                }
                log.info("已复制文章规则配置，数量：{}", templateArticleConfigs.size());
            }
        }

        log.info("批量生成域名配置完成，成功数量：{}", successCount);
        return successCount;
    }

    @Override
    public List<DomainConfig> findAllPhone(String phone, String domain) {
        // 查询所有配置
        List<DomainConfig> allConfigs = domainConfigMapper.selectList(
                new LambdaQueryWrapper<DomainConfig>()
                        .eq(DomainConfig::getContactPhone, phone)
                        .eq(StrUtil.isNotBlank(domain), DomainConfig::getDomain, domain)
                        .orderByDesc(DomainConfig::getId));
        allConfigs.stream().forEach(config -> {
            List<Integer> commentRuleIds = DomainCommentRuleMapper.selectList(
                    new LambdaQueryWrapper<DomainCommentRule>()
                            .in(DomainCommentRule::getDomainConfigId, config.getId()))
                    .stream()
                    .map(DomainCommentRule::getCommentRuleId)
                    .collect(Collectors.toList());

            List<Long> articleRuleIds = articleConfigMapper.selectList(
                    new LambdaQueryWrapper<DomainArticleConfig>()
                            .in(DomainArticleConfig::getDomainId, config.getId()))
                    .stream()
                    .map(DomainArticleConfig::getTypeId)
                    .collect(Collectors.toList());

            List<Integer> seoKeywordIds = DomainSeoKeywordMapper.selectList(
                    new LambdaQueryWrapper<DomainSeoKeyword>()
                            .in(DomainSeoKeyword::getDomainConfigId, config.getId()))
                    .stream()
                    .map(DomainSeoKeyword::getSeoKeywordId)
                    .collect(Collectors.toList());
            config.setRewriteRuleIds(seoKeywordIds);
            config.setCommentRuleIds(commentRuleIds);
            config.setArticleRuleIds(articleRuleIds.stream().map(Long::intValue).collect(Collectors.toList()));
        });

        return allConfigs;
    }

    // 这里需要随机生成备案号
    public String generateIcp() {
        return "ICP备" + generateRandomNumber(8) + "号";
    }

    // 这里需要随机生成版权信息
    public String generateCopyright(String domain) {
        return "© " + LocalDateTime.now().getYear() + " " + domain + " All Rights Reserved.";
    }

    // 这里需要随机生成联系电话
    public String generateContactPhone() {
        // 手机号前三位
        String[] prefixes = { "133", "138", "135", "139", "150", "151", "152", "157", "158", "159", "182", "183", "187",
                "188" };
        String prefix = prefixes[new Random().nextInt(prefixes.length)];

        // 随机生成后8位
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(random.nextInt(10));
        }

        return prefix + sb.toString();
    }

    // 这里需要随机生成联系邮箱
    public String generateContactEmail() {
        // 邮箱前缀长度随机6-10位
        int length = new Random().nextInt(5) + 6;
        StringBuilder prefix = new StringBuilder();
        Random random = new Random();

        // 生成随机字母和数字组合
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            prefix.append(chars.charAt(random.nextInt(chars.length())));
        }

        // 邮箱后缀域名随机选择
        String[] domains = { "@qq.com", "@163.com", "@gmail.com", "@hotmail.com", "@outlook.com" };
        String suffix = domains[random.nextInt(domains.length)];

        return prefix.toString() + suffix;
    }

    // 随机生成连系地址
    public String generateContactAddress() {
        // 省份
        String[] provinces = { "北京市", "上海市", "广东省", "浙江省", "江苏省" };
        // 城市
        String[] cities = { "朝阳区", "海淀区", "浦东新区", "深圳市", "杭州市", "南京市" };
        // 详细地址
        String[] streets = { "科技园路", "中关村大街", "长安街", "人民路", "解放路" };
        String[] buildings = { "1号楼", "2号楼", "A座", "B座", "科技大厦" };

        Random random = new Random();
        String province = provinces[random.nextInt(provinces.length)];
        String city = cities[random.nextInt(cities.length)];
        String street = streets[random.nextInt(streets.length)];
        String building = buildings[random.nextInt(buildings.length)];

        return province + city + street + random.nextInt(100) + "号" + building;
    }

    @Override
    public void saveCommentConfig(List<Integer> domainIds, String phone, List<Integer> selectedRules,
            boolean overwriteExisting) {
        // 删除所有已存在的评论规则配置
        DomainCommentRuleMapper.delete(new LambdaQueryWrapper<DomainCommentRule>()
                .in(DomainCommentRule::getDomainConfigId, domainIds));

        // 插入新的评论规则配置
        for (Integer domainId : domainIds) {
            for (Integer ruleId : selectedRules) {
                DomainCommentRule commentRule = new DomainCommentRule();
                commentRule.setDomainConfigId(domainId);
                commentRule.setCommentRuleId(ruleId);
                CompletableFuture.runAsync(() -> {
                    DomainCommentRuleMapper.insert(commentRule);
                });
            }
        }
    }

    @Override
    public void saveKeywordConfig(List<Integer> domainIds, String phone, List<Integer> selectedRules,
            boolean overwriteExisting) {
        // 删除所有已存在的SEO关键词配置
        DomainSeoKeywordMapper.delete(new LambdaQueryWrapper<DomainSeoKeyword>()
                .in(DomainSeoKeyword::getDomainConfigId, domainIds));
        // 插入新的SEO关键词配置
        for (Integer domainId : domainIds) {
            for (Integer ruleId : selectedRules) {
                DomainSeoKeyword seoKeyword = new DomainSeoKeyword();
                seoKeyword.setDomainConfigId(domainId);
                seoKeyword.setSeoKeywordId(ruleId);
                
                CompletableFuture.runAsync(() -> {
                    DomainSeoKeywordMapper.insert(seoKeyword);
                });
            }
        }
    }

    @Override
    public void saveArticleConfig(List<Integer> domainIds, String phone, List<Integer> selectedRules,
            boolean overwriteExisting) {
        // 删除所有已存在的文章规则配置
        articleConfigMapper.delete(new LambdaQueryWrapper<DomainArticleConfig>()
                .in(DomainArticleConfig::getDomainId, domainIds));
        // 插入新的文章规则配置
        for (Integer domainId : domainIds) {
            for (Integer ruleId : selectedRules) {
                DomainArticleConfig articleConfig = new DomainArticleConfig();
                articleConfig.setDomainId(Long.valueOf(domainId));
                articleConfig.setTypeId(Long.valueOf(ruleId));
                articleConfig.setSort(0);
                CompletableFuture.runAsync(() -> {
                    articleConfigMapper.insert(articleConfig);
                });
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchDeleteResult batchDeleteByPhone(String phone, List<Integer> domainIds) {
        log.info("开始批量删除操作，手机号: {}, 域名ID列表: {}", phone, domainIds);
        
        BatchDeleteResult result = new BatchDeleteResult();
        result.setDeletedDomains(new ArrayList<>());
        result.setFailedDomains(new ArrayList<>());
        
        try {
            // 1. 获取到这个批次的所有域名
            List<DomainConfig> domainsToDelete;
            if (domainIds != null && !domainIds.isEmpty()) {
                // 如果指定了域名ID，则只删除指定的域名
                LambdaQueryWrapper<DomainConfig> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(DomainConfig::getContactPhone, phone)
                           .in(DomainConfig::getId, domainIds);
                domainsToDelete = domainConfigMapper.selectList(queryWrapper);
            } else {
                // 如果没有指定域名ID，则删除该手机号下所有域名
                LambdaQueryWrapper<DomainConfig> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(DomainConfig::getContactPhone, phone);
                domainsToDelete = domainConfigMapper.selectList(queryWrapper);
            }
            
            if (domainsToDelete.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("未找到要删除的域名配置");
                return result;
            }
            
            log.info("找到 {} 个域名配置需要删除", domainsToDelete.size());
            
            int deletedCount = 0;
            
            for (DomainConfig domainConfig : domainsToDelete) {
                try {
                    // 执行单个域名的删除操作
                    deleteSingleDomainConfig(domainConfig);
                    
                    result.getDeletedDomains().add(domainConfig.getDomain());
                    deletedCount++;
                    
                    log.info("成功删除域名配置: {} (ID: {})", domainConfig.getDomain(), domainConfig.getId());
                    
                } catch (Exception e) {
                    log.error("删除域名配置失败: {} (ID: {}), 错误: {}", 
                             domainConfig.getDomain(), domainConfig.getId(), e.getMessage(), e);
                    result.getFailedDomains().add(domainConfig.getDomain() + ": " + e.getMessage());
                }
            }
            
            result.setDeletedCount(deletedCount);
            result.setSuccess(deletedCount > 0);
            
            if (result.getFailedDomains().isEmpty()) {
                result.setMessage(String.format("批量删除成功，共删除 %d 个域名配置", deletedCount));
            } else {
                result.setMessage(String.format("批量删除完成，成功删除 %d 个，失败 %d 个", 
                                               deletedCount, result.getFailedDomains().size()));
            }
            
            log.info("批量删除操作完成，成功: {}, 失败: {}", deletedCount, result.getFailedDomains().size());
            
        } catch (Exception e) {
            log.error("批量删除操作失败，手机号: {}", phone, e);
            result.setSuccess(false);
            result.setMessage("批量删除操作失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 删除单个域名配置及其所有关联数据
     * 按照指定的删除逻辑执行：
     * 1. 获取域名信息
     * 2. 删除域名配置【关键词，评论，文章】
     * 3. 删除域名配置
     * 4. 删除文章和域名的绑定关系
     * 5. 删除域名记录
     */
    private void deleteSingleDomainConfig(DomainConfig domainConfig) {
        Integer domainId = domainConfig.getId();
        String domain = domainConfig.getDomain();
        
        log.info("开始删除域名配置: {} (ID: {})", domain, domainId);
        
        // 2. 根据域名查询域名配置【关键词，评论，文章】并删除配置内容
        
        // 删除关联的SEO关键词配置
        log.debug("删除SEO关键词配置，域名ID: {}", domainId);
        DomainSeoKeywordMapper.delete(new LambdaQueryWrapper<DomainSeoKeyword>()
                .eq(DomainSeoKeyword::getDomainConfigId, domainId));
        
        // 删除关联的评论规则配置
        log.debug("删除评论规则配置，域名ID: {}", domainId);
        DomainCommentRuleMapper.delete(new LambdaQueryWrapper<DomainCommentRule>()
                .eq(DomainCommentRule::getDomainConfigId, domainId));
        
        // 删除关联的文章分配规则配置
        log.debug("删除文章分配规则配置，域名ID: {}", domainId);
        articleConfigMapper.delete(new LambdaQueryWrapper<DomainArticleConfig>()
                .eq(DomainArticleConfig::getDomainId, domainId));
        
        // 4. 删除文章和域名的绑定关系（将文章的domain_config字段设置为null）
        log.debug("解除文章和域名的绑定关系，域名: {}", domain);
        jdbcTemplate.update("UPDATE wf_news SET domain_config = NULL WHERE domain_config = ?", domain);
        
        // 3. 删除域名配置
        log.debug("删除域名配置记录，域名ID: {}", domainId);
        domainConfigMapper.deleteById(domainId);
        
        log.info("成功删除域名配置及所有关联数据: {} (ID: {})", domain, domainId);
    }

}
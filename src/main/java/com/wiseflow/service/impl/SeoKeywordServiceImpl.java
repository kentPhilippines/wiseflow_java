package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.entity.DomainSeoKeyword;
import com.wiseflow.entity.SeoKeyword;
import com.wiseflow.mapper.DomainSeoKeywordMapper;
import com.wiseflow.mapper.SeoKeywordMapper;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.service.SeoKeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * SEO关键词服务实现类
 */
@Service
public class SeoKeywordServiceImpl extends ServiceImpl<SeoKeywordMapper, SeoKeyword> implements SeoKeywordService {

    @Autowired
    private SeoKeywordMapper seoKeywordMapper;

    @Autowired
    private DomainConfigService domainConfigService;

    @Autowired
    private DomainSeoKeywordMapper domainSeoKeywordMapper;

    @Override
    public long count() {
        return baseMapper.selectCount(null);
    }

    @Override
    public List<SeoKeyword> findAll() {
        return this.lambdaQuery().list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeoKeyword save1(SeoKeyword keyword) {
        if (keyword.getId() == null) {
            baseMapper.insert(keyword);
        } else {
            baseMapper.updateById(keyword);
        }
        return keyword;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SeoKeyword> batchSave(List<SeoKeyword> keywords) {
        saveBatch(keywords);
        return keywords;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SeoKeyword keyword) {
        updateById(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        baseMapper.batchDelete(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Boolean enabled) {
        baseMapper.batchUpdateStatus(ids, enabled);
    }

    @Override
    public List<SeoKeyword> findEnabled() {
        return this.lambdaQuery().eq(SeoKeyword::getEnabled, true).list();
    }

    @Override
    public List<SeoKeyword> findByType(Integer type) {
        return this.lambdaQuery().eq(SeoKeyword::getType, type).list();
    }

    @Override
    public List<SeoKeyword> findByUseScene(Integer useScene) {
        return this.lambdaQuery().eq(SeoKeyword::getUseScene, useScene).list();
    }

    @Override
    public SeoKeyword findById(Long id) {
        return getById(id);
    }

    @Override
    public List<SeoKeyword> findByStatus(Boolean enabled) {
        return this.lambdaQuery().eq(SeoKeyword::getEnabled, enabled).list();
    }
    @Override
    public List<SeoKeyword> getKeywordsByDomain(String domain) {

        //查询域名id
        Optional<DomainConfig> byDomain = domainConfigService.findByDomain(domain);
        DomainConfig domainConfig = byDomain.get();


        //查询中间表
        List<DomainSeoKeyword> domainSeoKeywords = domainSeoKeywordMapper.selectList(
            new QueryWrapper<DomainSeoKeyword>()
                .eq("domain_config_id", domainConfig.getId())
        );


        //查询所有的
        List<SeoKeyword> seoKeywords = seoKeywordMapper.selectList(
            new QueryWrapper<SeoKeyword>()
                .eq("enabled", true)
                .orderByAsc("id")
        );

        //设置是否已插入
        seoKeywords.forEach(seoKeyword -> {
            seoKeyword.setIsInserted(domainSeoKeywords.stream().anyMatch(domainSeoKeyword -> domainSeoKeyword.getSeoKeywordId() == seoKeyword.getId()));
        });


        return seoKeywords;


 
    }
} 
package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.entity.ArticleRule;
import com.wiseflow.entity.DomainArticleConfig;
import com.wiseflow.mapper.ArticleRuleMapper;
import com.wiseflow.entity.DomainConfig;
import com.wiseflow.mapper.DomainArticleRuleMapper;
import com.wiseflow.service.ArticleRuleService;
import com.wiseflow.service.DomainConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 文章分配规则Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRuleServiceImpl extends ServiceImpl<ArticleRuleMapper, ArticleRule> implements ArticleRuleService {

    private final ArticleRuleMapper articleRuleMapper;
    private final DomainConfigService domainConfigService;
    private final DomainArticleRuleMapper domainArticleRuleMapper;

    @Override
    public List<ArticleRule> findAll() {
        LambdaQueryWrapper<ArticleRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ArticleRule::getSort);
        return articleRuleMapper.selectList(wrapper);
    }



    @Override
    public ArticleRule findById(Long id) {
        return articleRuleMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean save(ArticleRule rule) {
        return super.save(rule);
    }

    @Override
    public long count() {
        return super.count();
    }

    @Override
    @Transactional
    public ArticleRule update(ArticleRule rule) {
        articleRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        articleRuleMapper.deleteById(id);
    }

    @Override
    public List<ArticleRule> getArticlesByDomain(String domain) {
        //查询域名id
        Optional<DomainConfig> byDomain = domainConfigService.findByDomain(domain);
        DomainConfig domainConfig = byDomain.get();



        //查询中间表
        List<DomainArticleConfig> domainArticleRules;
        domainArticleRules = domainArticleRuleMapper.selectList(
            new QueryWrapper<DomainArticleConfig>()
                .eq("domain_id", domainConfig.getId())
        );


        //查询所有的
        List<ArticleRule> articleRules = articleRuleMapper.selectList(
            new QueryWrapper<ArticleRule>()
                .eq("enabled", true)
                .orderByAsc("id")
        );

        //设置是否已插入
        articleRules.forEach(articleRule -> {
            articleRule.setIsInserted(domainArticleRules.stream().anyMatch(domainArticleRule -> domainArticleRule.getTypeId() == articleRule.getId()));
        });

        return articleRules;






    }
} 
package com.wiseflow.controller;

import com.wiseflow.entity.Activity;
import com.wiseflow.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private DomainConfigService domainConfigService;

    @Autowired
    private CommentRuleService commentRuleService;

    @Autowired
    private SeoKeywordService seoKeywordService;

 

    @Autowired
    private ActivityService activityService;



    @Autowired
    private ArticleRuleService articleRuleService;
    @Autowired
    private NewsService newsService;

    @GetMapping("/index")
    public String index(Model model) {
        // 获取域名总数
        Long domainCount = domainConfigService.count();
        model.addAttribute("domainCount", domainCount);

        // 获取启用的域名数量
        Long enabledDomainCount = domainConfigService.countEnabled();
        model.addAttribute("enabledDomainCount", enabledDomainCount);

        // 获取规则总数（评论规则 + SEO关键词规则）
        long commentRuleCount = commentRuleService.count();
        long seoKeywordCount = seoKeywordService.count();

        long articleRuleCount = articleRuleService.count();


        Long ruleCount = commentRuleCount + seoKeywordCount + articleRuleCount  ;
        model.addAttribute("ruleCount", ruleCount);






        // 获取未分配文章数
        Long unassignedNewsCount = newsService.countUnassignedNews();
        model.addAttribute("unassignedNewsCount", unassignedNewsCount);

        // 获取总文章数
        long totalNewsCount = newsService.count();
        model.addAttribute("totalNewsCount", totalNewsCount);

        // 获取关键词规则数
        model.addAttribute("keywordRuleCount", seoKeywordCount);

        // 获取文章规则数
        model.addAttribute("articleRuleCount", articleRuleCount);

        // 获取评论规则数
        model.addAttribute("commentRuleCount", commentRuleCount);

        // 获取最近的活动记录（最新的10条）
        List<Activity> recentActivities = activityService.getRecentActivities(10);
        model.addAttribute("recentActivities", recentActivities);

        return "index";
    }


    @GetMapping("/comment/rule")
    public String commentRule(Model model) {
        return "comment-rule";
    }



    @GetMapping("/seo-keywords")
    public String seoKeyword(Model model) {
        return "seo-keyword";
    }   

    @GetMapping("/article/rule")
    public String articleRule(Model model) {
        /**
         * 这里需要查询  新闻表中 每个类型文章的总数 和目前已经分配的总数 展示在前端
         */
        // 查询每个类型文章的总数
        List<Map<String, Object>> articleCounts = newsService.getArticleCounts();
        model.addAttribute("articleCounts", articleCounts);
        return "article-rule";
    }   

    @GetMapping("/rule/assign")
    public String ruleAssign(Model model) {
        return "rule-assign";
    }



    @GetMapping("/domain")
    public String domain(Model model) {
        return "domain";
    }
} 
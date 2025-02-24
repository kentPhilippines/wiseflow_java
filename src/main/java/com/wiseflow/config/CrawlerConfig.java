package com.wiseflow.config;

import com.wiseflow.model.CrawlConfig;
import com.wiseflow.model.ParseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CrawlerConfig {

    @Bean
    public List<CrawlConfig> crawlConfigs() {
        List<CrawlConfig> configs = new ArrayList<>();

        // 网易体育配置
        //  configs.add(createNetEaseConfig());

        // 唯彩看球配置
        configs.add(createVipcConfig());

        // 直播吧足球配置
        configs.add(createZhibo8Config());

        return configs;
    }

    private CrawlConfig createNetEaseConfig() {
        CrawlConfig config = new CrawlConfig();
        config.setName("网易体育");
        config.setUrl("https://sports.163.com/");
        config.setEnabled(true);
        config.setEncoding("UTF-8");
        config.setTimeout(10000);
        config.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/91.0.4472.124");

        ParseRule rule = new ParseRule();
        rule.setTitleSelector("h1.post_title, h1.title");
        rule.setContentSelector("#endText, .post_body");
        rule.setAuthorSelector(".post_author, .ep-editor");
        rule.setImageSelector("#endText img, .post_body img");
        rule.setCategoryName("网易体育");

        config.setParseRule(rule);
        return config;
    }

    private CrawlConfig createVipcConfig() {
        CrawlConfig config = new CrawlConfig();
        config.setName("唯彩看球");
        config.setUrl("https://www.vipc.cn/digit");
        config.setEnabled(true);
        config.setEncoding("UTF-8");
        config.setTimeout(10000);
        config.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        ParseRule rule = new ParseRule();
        rule.setTitleSelector(".vMod_article_title");
        rule.setContentSelector(".vMod_article_content");
        rule.setAuthorSelector("div.article-info");
        rule.setImageSelector(".vMod_article_content img[src]");
        rule.setCategoryName("彩票资讯");

        config.setParseRule(rule);
        return config;
    }

    private CrawlConfig createZhibo8Config() {
        CrawlConfig config = new CrawlConfig();
        config.setName("直播吧足球");
        config.setUrl("https://news.zhibo8.com/zuqiu/more.htm");
        config.setEnabled(true);
        config.setEncoding("UTF-8");
        config.setTimeout(10000);
        config.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        ParseRule rule = new ParseRule();
        rule.setTitleSelector("h1");
        rule.setContentSelector(".content");
        rule.setAuthorSelector(".author");
        rule.setImageSelector(".content img[src]");
        rule.setCategoryName("直播吧足球");

        config.setParseRule(rule);
        return config;
    }
} 
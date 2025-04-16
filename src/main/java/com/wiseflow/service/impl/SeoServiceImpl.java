package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wiseflow.entity.News;
import com.wiseflow.entity.SeoKeyword;
import com.wiseflow.mapper.SeoKeywordMapper;
import com.wiseflow.service.SeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SeoServiceImpl implements SeoService {

    private final SeoKeywordMapper seoKeywordMapper;
    private final Random random = new Random();

    @Override
    public List<SeoKeyword> getEnabledKeywords(String domain) {
        LambdaQueryWrapper<SeoKeyword> wrapper = new LambdaQueryWrapper<>();
        wrapper
               .eq(SeoKeyword::getEnabled, true)
               .orderByDesc(SeoKeyword::getCreateTime);
        return seoKeywordMapper.selectList(wrapper);
    }

    @Override
    public News processSeoOptimization(News news) {
        List<SeoKeyword> keywords = getEnabledKeywords(news.getDomainConfig());
        if (keywords.isEmpty()) {
            return news;
        }

        // 处理标题
        String optimizedTitle = processTitle(news.getTitle(), keywords);
        news.setTitle(optimizedTitle);

        // 处理内容
        String optimizedContent = processContent(news.getContent().getContent(), keywords);
        news.getContent().setContent(optimizedContent);

        return news;
    }

    private String processTitle(String title, List<SeoKeyword> keywords) {
        if (title == null || title.isEmpty()) {
            return title;
        }

        String result = title;
        for (SeoKeyword keyword : keywords) {
            if (!keyword.getAllowTitle()) {
                continue;
            }

            // 检查标题中是否已包含关键词

        }
        return result;
    }

    private String processContent(String content, List<SeoKeyword> keywords) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        Document doc = Jsoup.parse(content);
        
        // 获取所有文本节点
        Elements paragraphs = doc.select("p");
        Map<SeoKeyword, Integer> insertionCounts = new HashMap<>();

        for (Element p : paragraphs) {
            for (TextNode textNode : p.textNodes()) {
                String text = textNode.text();
                
                for (SeoKeyword keyword : keywords) {
                    int currentInsertions = insertionCounts.getOrDefault(keyword, 0);
                    if (currentInsertions >= keyword.getMaxInsertions()) {
                        continue;
                    }

                    // 检查是否适合插入关键词
                    if (true) {
                        String optimizedText = insertKeyword(text, keyword);
                        textNode.text(optimizedText);
                        insertionCounts.put(keyword, currentInsertions + 1);
                    }
                }
            }
        }

        return doc.body().html();
    }

    private boolean isSuitableForInsertion(String text, String keyword) {
        // 检查文本长度是否足够
        if (text.length() < 20) {
            return false;
        }

        // 检查是否已包含关键词
        if (text.contains(keyword)) {
            return false;
        }

        // 检查是否包含标点符号（可以在标点后插入）
        return text.matches(".*[。，！？；].*");
    }

    private String insertKeyword(String text, SeoKeyword keyword) {
        // 找到所有标点符号的位置
        List<Integer> punctuationPositions = new ArrayList<>();
        Pattern pattern = Pattern.compile("[。，！？；]");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            punctuationPositions.add(matcher.start());
        }

        if (punctuationPositions.isEmpty()) {
            return text;
        }

        // 随机选择一个标点符号后插入关键词
        int insertPosition = punctuationPositions.get(random.nextInt(punctuationPositions.size())) + 1;


        return text.substring(0, insertPosition)  + text.substring(insertPosition);
    }

    @Override
    public SeoKeyword saveKeyword(SeoKeyword keyword) {
        if (keyword.getId() == null) {
            seoKeywordMapper.insert(keyword);
        } else {
            seoKeywordMapper.updateById(keyword);
        }
        return keyword;
    }

    @Override
    public void deleteKeyword(Integer id) {
        seoKeywordMapper.deleteById(id);
    }
} 
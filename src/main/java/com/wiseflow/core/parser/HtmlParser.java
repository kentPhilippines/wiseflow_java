package com.wiseflow.core.parser;

import com.wiseflow.entity.Article;
import com.wiseflow.entity.Category;
import com.wiseflow.model.ParseRule;
import com.wiseflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HtmlParser implements Parser {
    private final CategoryService categoryService;
    
    @Override
    public Article parse(Document doc, ParseRule rule) {
        try {
            // 解析标题
            Element titleElement = doc.select(rule.getTitleSelector()).first();
            if (titleElement == null || !StringUtils.hasText(titleElement.text().trim())) {
                log.warn("Failed to extract title from: {}", doc.location());
                return null;
            }

            Article article = new Article();
            article.setTitle(titleElement.text().trim());
            
            // 解析内容
            Element contentElement = doc.select(rule.getContentSelector()).first();
            if (contentElement == null || !StringUtils.hasText(contentElement.text().trim())) {
                log.warn("Failed to extract content from: {}", doc.location());
                return null;
            }
            
            // 根据不同网站进行内容清理
            String baseUrl = doc.baseUri();
            if (baseUrl.contains("vipc.cn")) {
                // 唯彩看球特殊处理
                cleanVipcContent(contentElement);
            } else if (baseUrl.contains("163.com")) {
                // 网易体育特殊处理
                cleanNetEaseContent(contentElement);
            }
            
            // 处理内容中的图片
            processImages(article, contentElement, baseUrl);
            
            // 获取纯文本内容并清理
            String content = processContent(contentElement, baseUrl);
            if (StringUtils.hasText(content)) {
                article.setContent(content);
            } else {
                log.warn("Empty content after processing: {}", doc.location());
                return null;
            }
            
            // 解析作者
            processAuthor(article, doc, rule, baseUrl);
            
            // 设置基本信息
            article.setUrl(doc.location());
            article.setCategoryName(rule.getCategoryName());
            article.setCrawlTime(LocalDateTime.now());
            article.setSummaryFromContent();
            
            return article;
        } catch (Exception e) {
            log.error("Failed to parse article: {}", doc.location(), e);
            return null;
        }
    }
    
    private boolean isValidArticle(Article article) {
        return StringUtils.hasText(article.getTitle()) &&
               StringUtils.hasText(article.getContent()) &&
               StringUtils.hasText(article.getUrl()) &&
               StringUtils.hasText(article.getCategoryName());
    }
    
    private Category extractCategory(Document doc, ParseRule rule) {
        try {
            String categoryName;
            if (StringUtils.hasText(rule.getCategorySelector())) {
                // 从页面提取分类
                categoryName = extractText(doc, rule.getCategorySelector(), null);
            } else {
                // 使用默认分类
                categoryName = rule.getCategoryName();
            }
            
            if (!StringUtils.hasText(categoryName)) {
                categoryName = "未分类";
            }
            
            // 获取或创建分类
            Category category = categoryService.findByName(categoryName);
            if (category == null) {
                category = new Category();
                category.setName(categoryName);
                category.setDescription("自动创建的分类");
                category = categoryService.save(category);
            }
            
            return category;
        } catch (Exception e) {
            log.error("Failed to extract category", e);
            return null;
        }
    }

    @Override
    public String extractTitle(Element doc, String selector) {
        return extractText(doc, selector, null);
    }

    @Override
    public String extractContent(Element doc, String selector) {
        Element contentElement = doc.select(selector).first();
        if (contentElement == null) {
            return "";
        }

        // 移除无关元素
        contentElement.select(".post_recommend, .post_share, .post_comment, .ep-source," + 
                            ".sports-footer, .sports-copyright, script, style").remove();

        StringBuilder content = new StringBuilder();
        
        // 处理正文段落
        Elements paragraphs = contentElement.select("p");
        if (!paragraphs.isEmpty()) {
            for (Element p : paragraphs) {
                if (isValidParagraph(p)) {
                    String text = p.text().trim();
                    if (StringUtils.hasText(text)) {
                        content.append(text).append("\n");
                    }
                }
            }
        }

        // 处理体育比分和数据表格
        Elements tables = contentElement.select("table.sports-data");
        if (!tables.isEmpty()) {
            for (Element table : tables) {
                content.append(table.text()).append("\n");
            }
        }

        return content.toString().trim();
    }

    private boolean isValidParagraph(Element element) {
        // 排除常见的无关元素
        String classNames = element.attr("class").toLowerCase();
        String id = element.attr("id").toLowerCase();
        
        // 网易新闻特有的广告和推荐类
        String[] excludeKeywords = {
            "post_recommend", "post_share", "post_comment",
            "ep-source", "post_author", "post_time",
            "广告", "分享", "评论", "推荐", "版权声明"
        };
        
        for (String keyword : excludeKeywords) {
            if (classNames.contains(keyword) || id.contains(keyword)) {
                return false;
            }
        }
        
        // 检查父元素
        Element parent = element.parent();
        if (parent != null) {
            String parentClass = parent.attr("class").toLowerCase();
            for (String keyword : excludeKeywords) {
                if (parentClass.contains(keyword)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private String extractText(Element doc, String selector, String defaultValue) {
        if (!StringUtils.hasText(selector)) {
            return defaultValue;
        }
        
        Element element = doc.select(selector).first();
        if (element == null) {
            return defaultValue;
        }
        
        // 移除script和style标签
        element.select("script, style").remove();
        
        String text = element.text().trim();
        return StringUtils.hasText(text) ? text : defaultValue;
    }

    private List<String> extractImages(Element doc, String selector) {
        List<String> images = new ArrayList<>();
        if (!StringUtils.hasText(selector)) {
            return images;
        }
        
        Elements elements = doc.select(selector);
        for (Element img : elements) {
            String src = img.attr("src");
            if (StringUtils.hasText(src)) {
                if (src.startsWith("//")) {
                    src = "https:" + src;
                }
                images.add(src);
            }
        }
        return images;
    }

    private void cleanVipcContent(Element contentElement) {
        // 移除广告和无关内容
        contentElement.select(".advertisement, .social-share, .related-articles, script, style").remove();
        // 移除底部的标签和推荐
        contentElement.select(".article-tags, .article-recommend, .related-recommend").remove();
        // 移除评论区
        contentElement.select("#comment-section, .comment-section").remove();
        // 移除上一期下一期链接
        contentElement.select(".prev-next-links").remove();
        // 移除热门标签
        contentElement.select(".hot-tags").remove();
        // 移除相关推荐
        contentElement.select(".related-articles, .related-recommend").remove();
        // 移除底部链接
        contentElement.select("a:contains(更多推荐)").remove();
        // 移除内部链接
        contentElement.select("a.kw").remove();
    }

    private void cleanNetEaseContent(Element contentElement) {
        // 移除网易特有的广告和推荐
        contentElement.select(".post_recommend, .post_share, .post_comment").remove();
        contentElement.select(".ep-source, .post_author, .post_time").remove();
        contentElement.select(".sports-footer, .sports-copyright").remove();
        contentElement.select(".ntes-nav, .ntes-footer").remove();
        contentElement.select("script, style").remove();
        // 移除分享和推荐
        contentElement.select(".share-join, .share-wrap, .recommend-wrap").remove();
        // 移除底部相关链接
        contentElement.select(".related-news, .related-link").remove();
    }

    private void processImages(Article article, Element contentElement, String baseUrl) {
        Elements images = contentElement.select("img[src]");
        images.forEach(img -> {
            String imgUrl = img.attr("abs:src");
            if (StringUtils.hasText(imgUrl)) {
                // 处理网易图片链接
                if (baseUrl.contains("163.com") && imgUrl.contains("?imageView")) {
                    imgUrl = imgUrl.substring(0, imgUrl.indexOf("?"));
                }
                article.addImage(imgUrl);
            }
        });
    }

    private String processContent(Element contentElement, String baseUrl) {
        String content = contentElement.text().trim()
            .replaceAll("\\s+", " ");  // 合并多个空白字符
        
        if (baseUrl.contains("vipc.cn")) {
            content = content.replaceAll("【.*?】", "")  // 移除方括号内容
                .replaceAll("更多优质推荐：.*", "")  // 移除推荐内容
                .replaceAll("上一篇:.*", "")  // 移除上一篇
                .replaceAll("下一篇:.*", "")  // 移除下一篇
                .replaceAll("热门栏目.*", "")  // 移除热门栏目
                .replaceAll("\\d+\\s*评.*", "")  // 移除评论数
                .replaceAll("双色球\\d+期专家精准预测号之.*预测", "")  // 移除预测标题
                .replaceAll("双色球红球公式绝杀：", "")  // 移除固定文本
                .replaceAll("双色球蓝球公式绝杀：", "")  // 移除固定文本
                .replaceAll("彩妹个人心水推荐：", "");  // 移除固定文本
        }
        
        return content.trim();
    }

    private void processAuthor(Article article, Document doc, ParseRule rule, String baseUrl) {
        Element authorElement = doc.select(rule.getAuthorSelector()).first();
        if (authorElement != null) {
            String author = authorElement.text();
            
            if (baseUrl.contains("vipc.cn")) {
                author = author.replace("作者：", "")
                    .replace("来源：", "")
                    .replace("热门栏目", "")
                    .replace("今天", "")
                    .replace("昨天", "")
                    .replaceAll("\\d{2}:\\d{2}", "")  // 移除时间
                    .trim();
            } else if (baseUrl.contains("163.com")) {
                author = author.replace("责任编辑：", "")
                    .replace("作者：", "")
                    .replaceAll("\\s*[|｜].*", "")  // 移除分隔符后的内容
                    .trim();
            }
            
            if (StringUtils.hasText(author)) {
                article.setAuthor(author);
            }
        }
    }
} 
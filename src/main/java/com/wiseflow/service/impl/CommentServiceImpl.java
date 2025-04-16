package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.config.CommentConfig;
import com.wiseflow.entity.*;
import com.wiseflow.mapper.CommentMapper;
import com.wiseflow.mapper.DomainConfigMapper;
import com.wiseflow.service.CommentRuleService;
import com.wiseflow.service.CommentService;
import com.wiseflow.service.DomainConfigService;
import com.wiseflow.service.NewsService;
import com.wiseflow.service.SeoKeywordService;
import com.wiseflow.util.SportCommentAiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final NewsService newsService;
    private final CommentRuleService commentRuleService;
    private final SeoKeywordService seoKeywordService;
    private final DomainConfigMapper domainConfigMapper;
    private final  SportCommentAiUtil  sportCommentAiUtil;
    private final DomainConfigService domainConfigService;

    private final Random random = new Random();

    // 随机的评论者名称
    private static final String[] COMMENTER_NAMES = {
            "张三", "李四", "王五", "赵六", "田七", "周八", "吴九", "郑十",
            "小明", "小红", "小刚", "小丽", "小华", "小林", "小雪", "小梅",
            "阳光", "明月", "繁星", "流云", "清风", "细雨", "春风", "夏雨"
    };

    // 随机的评论模板
    private static final String[] COMMENT_TEMPLATES = {
            "这篇文章讲的%s真不错，学到了很多。",
            "看完之后对%s有了更深入的了解，谢谢分享！",
            "文章内容很充实，特别是关于%s的部分很精彩。",
            "一直想了解%s相关的知识，这篇文章帮了大忙。",
            "作者对%s的见解很独到，值得思考。",
            "写得真好，把%s讲得这么清楚。",
            "长知识了，原来%s还有这么多内容可以挖掘。",
            "分析得很到位，尤其是%s部分的内容很有启发性。",
            "非常实用的信息，对%s感兴趣的朋友可以看看。",
            "观点很新颖，对%s有了不一样的认识。"
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Comment save1(Comment comment) {
        if (comment.getCommentTime() == null) {
            comment.setCommentTime(LocalDateTime.now());
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        if (comment.getIsAiGenerated() == null) {
            comment.setIsAiGenerated(false);
        }

        save(comment);
        log.info("保存评论成功: id={}, newsId={}", comment.getId(), comment.getNewsId());
        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Comment comment) {
        updateById(comment);
        log.info("更新评论成功: id={}", comment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        removeById(id);
        log.info("删除评论成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Integer> ids) {
        baseMapper.batchDelete(ids);
        log.info("批量删除评论成功: ids={}", ids);
    }

    @Override
    public Comment findById(Integer id) {
        return getById(id);
    }

    @Override
    public List<Comment> findByNewsId(Integer newsId) {
        return baseMapper.findByNewsId(newsId);
    }

    @Override
    public IPage<Comment> findByNewsIdPaged(Integer newsId, Page<Comment> page) {
        return baseMapper.findByNewsIdPaged(page, newsId);
    }

    @Override
    public IPage<Comment> findByDomainConfigPaged(String domainConfig, Page<Comment> page) {
        return baseMapper.findByDomainConfigPaged(page, domainConfig);
    }

    @Override
    public List<Comment> findByKeywordId(Integer keywordId) {
        return baseMapper.findByKeywordId(keywordId);
    }

    @Override
    public IPage<Comment> findAiGeneratedPaged(String domainConfig, Page<Comment> page) {
        return baseMapper.findAiGeneratedPaged(page, domainConfig);
    }

    @Override
    public int countByNewsId(Integer newsId) {
        return baseMapper.countByNewsId(newsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Integer id) {
        baseMapper.incrementLikeCount(id);
        log.info("点赞评论成功: id={}", id);
    }

    @Override
    public void generateAiComments(Integer newsId, int count, List<CommentRule> byDomainConfig ) {
      // 获取文章信息
        News news = newsService.findById(newsId)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + newsId));
        DomainConfig domainConfig = domainConfigMapper.selectByDomain(news.getDomainConfig());
        // 获取评论规则
        byDomainConfig.forEach(rule -> {
            if (rule == null || !rule.getEnabled() || !rule.getEnableAiComment()) {
                log.info("该域名未启用AI评论或评论规则不存在: {}", news.getDomainConfig());
                return;
            }
            //获取当前域名的关键词规则
            List<SeoKeyword> allDomainKeywords = domainConfigService.getAllDomainKeywords(domainConfig);
            List<String> keywords1 = domainConfig.getKeywordsSeo();
            if(keywords1.isEmpty()){
                log.info("该域名没有可用的SEO关键词: {}", news.getDomainConfig());
                return;
            }
            // 获取可用的SEO关键词
            List<SeoKeyword> keywords = new ArrayList<>();
            
            // 从所有域名关键词中筛选出评论场景可用的关键词
            for (SeoKeyword keyword : allDomainKeywords) {
                // useScene=2表示仅评论使用,useScene=3表示两者都可使用
                if (keyword.getUseScene() == 2 || keyword.getUseScene() == 3) {
                    String k = keywords1.get(random.nextInt(keywords1.size()));
                    keyword.setKeyword(k);
                    keywords.add(keyword);
                    log.info("该域名可用于评论的关键词: {}", keyword.getKeyword());
                }
            }

           

            if (keywords.isEmpty()) {
                log.info("该域名没有可用于评论的关键词: domainConfig={}", domainConfig.getDomain());
                return;
            }
            // 计算需要包含关键词的评论数量
            int keywordCommentCount = Math.round(count * rule.getKeywordIncludeRate() / 100f);

            // 计算使用AI生成的评论比例（固定10%）
            int aiCommentCount = Math.round(count * 0.1f);
            List<Comment> comments = new ArrayList<>();

            try {
                // 生成包含关键词的评论
                for (int i = 0; i < count; i++) {
                    Comment comment;
                    SeoKeyword keyword = keywords.get(random.nextInt(keywords.size()));

                    // 随机决定是使用AI生成评论还是使用模板生成评论
                    boolean useAi = i < aiCommentCount;

                    // 是否需要包含关键词
                    boolean needKeyword = i < keywordCommentCount;

                    if (needKeyword) {
                        // 生成包含关键词的评论
                        comment = generateCommentWithKeyword(news, keyword, rule, useAi);
                    } else {
                        // 生成普通评论
                        comment = generateNormalComment(news, rule, useAi);
                    }
                    comments.add(comment);
                }
            } catch (Exception e) {
                log.error("生成评论过程中出错: {}", e.getMessage());
                // 如果过程中出错，确保至少有一些评论
                if (comments.isEmpty()) {
                    // 紧急情况下生成几条基本评论
                    for (int i = 0; i < Math.min(3, count); i++) {
                        SeoKeyword keyword = !keywords.isEmpty() ?
                                keywords.get(random.nextInt(keywords.size())) : null;

                        Comment comment = new Comment();
                        comment.setNewsId(news.getId());
                        comment.setContent("不错的文章，很有意思！");
                        comment.setCommenterName(COMMENTER_NAMES[random.nextInt(COMMENTER_NAMES.length)]);
                        comment.setCommentTime(generateRandomCommentTime(news, rule));
                        comment.setLikeCount(random.nextInt(5));
                        if (keyword != null) comment.setSeoKeywordId(keyword.getId());
                        comment.setIsAiGenerated(true);
                        comment.setDomainConfig(news.getDomainConfig());
                        comments.add(comment);
                    }
                }
            }

            // 批量保存评论
         //   saveBatch(comments);
            for (Comment comment : comments) {
                log.info("成功为文章生成{}条AI评论: newsId={}", comments.size(), newsId);
                log.info("评论内容: {}", comment.getContent());
                log.info("评论者名称: {}", comment.getCommenterName());
                log.info("评论时间: {}", comment.getCommentTime());
                log.info("点赞数: {}", comment.getLikeCount());
                log.info("是否AI生成: {}", comment.getIsAiGenerated());
                log.info("域名配置: {}", comment.getDomainConfig());
                log.info("SEO关键词ID: {}", comment.getSeoKeywordId());
                this.save1(comment);
            }

            log.info("成功为文章生成{}条AI评论: newsId={}", comments.size(), newsId);
        });
    }

    /**
     * 生成包含关键词的评论
     */
    private Comment generateCommentWithKeyword(News news, SeoKeyword keyword, CommentRule rule, boolean useAi) {
        String content;
        if (useAi) {
            try {
                String category = news.getCategoryName() != null ? news.getCategoryName() : "新闻";
                content = sportCommentAiUtil.generateSportComment(news.getTitle(), category);
                // 确保AI生成的评论包含关键词
                if (!content.contains(keyword.getKeyword())) {
                    content = insertKeywordIntoContent(content, keyword);
                }
            } catch (Exception e) {
                log.warn("AI生成评论失败，使用模板: {}", e.getMessage());
                content = generateTemplateCommentWithKeyword(news, keyword);
            }
        } else {
            content = generateTemplateCommentWithKeyword(news, keyword);
        }

        Comment comment = new Comment();
        comment.setNewsId(news.getId());
        comment.setContent(content);
        comment.setCommentTime(generateRandomCommentTime(news, rule));
        comment.setLikeCount(generateRandomLikes());
        comment.setSeoKeywordId(keyword.getId());
        comment.setIsAiGenerated(useAi);
        comment.setDomainConfig(news.getDomainConfig());
        comment.setCommenterName(generateRandomCommenterName());

        return comment;
    }

    /**
     * 生成随机点赞数
     */
    private int generateRandomLikes() {
        // 使用指数分布使小点赞数更常见
        double lambda = 3.0;
        int likes = (int) (Math.log(1 - random.nextDouble()) / -lambda);
        return Math.min(likes, 50); // 最多50个赞
    }

    /**
     * 根据关键词类型生成评论内容
     */
    private String generateTemplateCommentWithKeyword(News news, SeoKeyword keyword) {
        String keywordText = keyword.getKeyword();
        boolean isAuthor = isLikelyAuthorName(keywordText);
        
        if (isAuthor) {
            return generateAuthorComment(keywordText);
        } else {
            return generateContentKeywordComment(keywordText);
        }
    }

    /**
     * 判断关键词是否可能是作者名称
     */
    private boolean isLikelyAuthorName(String keyword) {
        // 1. 长度通常在2-4个字符
        if (keyword.length() >= 2 && keyword.length() <= 4) {
            // 2. 通常由汉字组成
            return keyword.matches("^[\u4e00-\u9fa5]{2,4}$");
        }
        return false;
    }

    /**
     * 生成针对作者的评论
     */
    private String generateAuthorComment(String authorName) {
        return String.format(CommentConfig.getRandomElement(CommentConfig.AUTHOR_COMMENT_TEMPLATES), authorName);
    }

    /**
     * 生成针对内容关键词的评论
     */
    private String generateContentKeywordComment(String keyword) {
        return String.format(CommentConfig.getRandomElement(CommentConfig.CONTENT_KEYWORD_TEMPLATES), keyword);
    }

    /**
     * 将关键词自然地插入到内容中
     */
    private String insertKeywordIntoContent(String content, SeoKeyword keyword) {
        String insertPhrase = String.format(
            CommentConfig.getRandomElement(CommentConfig.KEYWORD_INSERT_PHRASES), 
            keyword.getKeyword()
        );
                
        // 在句子开头或适当位置插入关键词
        int insertPoint = content.indexOf("，");
        if (insertPoint == -1) {
            return insertPhrase + content;
        } else {
            return content.substring(0, insertPoint + 1) + insertPhrase + 
                   content.substring(insertPoint + 1);
        }
    }

    /**
     * 生成普通评论
     */
    private Comment generateNormalComment(News news, CommentRule rule, boolean useAi) {
        String content;
        
        if (useAi) {
            try {
                String category = news.getCategoryName() != null ? news.getCategoryName() : "新闻";
                content = sportCommentAiUtil.generateSportComment(news.getTitle(), category);
            } catch (Exception e) {
                log.warn("AI生成普通评论失败，使用模板: {}", e.getMessage());
                content = generateTemplateComment(news);
            }
        } else {
            content = generateTemplateComment(news);
        }

        // 调整评论长度
        if (content.length() < rule.getMinCommentLength()) {
            content += "，" + generateRandomWords(rule.getMinCommentLength() - content.length());
        }

        if (content.length() > rule.getMaxCommentLength()) {
            content = content.substring(0, rule.getMaxCommentLength() - 3) + "...";
        }

        Comment comment = new Comment();
        comment.setNewsId(news.getId());
        comment.setContent(content);
        comment.setCommenterName(generateRandomCommenterName());
        comment.setCommentTime(generateRandomCommentTime(news, rule));
        comment.setLikeCount(generateRandomLikes());
        comment.setIsAiGenerated(useAi);
        comment.setDomainConfig(news.getDomainConfig());

        return comment;
    }

    /**
     * 生成模板评论
     */
    private String generateTemplateComment(News news) {
        int randomType = random.nextInt(6);
        String content;
        
        switch (randomType) {
            case 0: // 使用标准模板
                content = CommentConfig.getRandomElement(CommentConfig.STANDARD_COMMENT_TEMPLATES);
                break;
                
            case 1: // 引用文章标题的评论
                String title = news.getTitle();
                if (title.length() > 15) {
                    title = title.substring(0, 15) + "...";
                }
                content = String.format(
                    CommentConfig.getRandomElement(CommentConfig.TITLE_COMMENT_TEMPLATES), 
                    title
                );
                break;
                
            case 2: // 情感型评论
                content = CommentConfig.getRandomElement(CommentConfig.EMOTIONAL_COMMENT_TEMPLATES);
                break;
                
            case 3: // 问题型评论
                content = CommentConfig.getRandomElement(CommentConfig.QUESTION_COMMENT_TEMPLATES);
                break;
                
            case 4: // 组合型评论
                content = "文章的" + 
                         CommentConfig.getRandomElement(new String[]{"分析", "讲解", "见解", "观点", "思路"}) + 
                         "非常" + 
                         CommentConfig.getRandomElement(new String[]{"精彩", "出色", "优秀", "专业", "深刻"}) + 
                         "，" + 
                         CommentConfig.getRandomElement(CommentConfig.COMMENT_ADDITIONS);
                break;
                
            case 5: // 带文章主题的评论
                String category = news.getCategoryName();
                if (category == null || category.trim().isEmpty()) {
                    category = "这个主题";
                }
                content = String.format(
                    "关于%s的好文章，值得一读。希望能看到更多这样的内容！",
                    category
                );
                break;
                
            default:
                content = "这篇文章内容很充实，值得一读！";
        }
        
        // 随机添加额外元素
        if (random.nextFloat() < 0.3) {
            String addition = CommentConfig.getRandomElement(CommentConfig.COMMENT_ADDITIONS);
            content = random.nextBoolean() ? addition + content : content + " " + addition;
        }
        
        return content;
    }

    /**
     * 生成随机评论时间
     */
    private LocalDateTime generateRandomCommentTime(News news, CommentRule rule) {
        // 获取文章发布时间
        LocalDateTime publishTime = news.getPublishTime();
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 计算评论时间范围
        long timeRangeInHours = rule.getCommentTimeRange() != null ? rule.getCommentTimeRange() : 24;
        // 计算最大可用时间范围(不能超过当前时间)
        LocalDateTime maxTime = now.isBefore(publishTime.plusHours(timeRangeInHours)) ? 
                              now : publishTime.plusHours(timeRangeInHours);
        // 在发布时间和最大时间之间随机生成评论时间
        long diffSeconds = java.time.Duration.between(publishTime, maxTime).getSeconds();
        long randomSeconds = (long) (random.nextDouble() * diffSeconds);
        return publishTime.plusSeconds(randomSeconds);
    }

    /**
     * 生成随机文字填充
     */
    private String generateRandomWords(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            if (sb.length() > 0) {
                sb.append("，");
            }
            sb.append(CommentConfig.getRandomElement(CommentConfig.COMMENT_FILLERS));
        }
        return sb.toString();
    }

    /**
     * 生成随机评论者名称
     */
    private String generateRandomCommenterName() {
        int nameType = random.nextInt(5);
        
        switch (nameType) {
            case 0: // 标准名称
                return CommentConfig.getRandomElement(CommentConfig.STANDARD_NAMES);
                
            case 1: // 带特殊符号的名称
                String baseName = CommentConfig.getRandomElement(CommentConfig.STANDARD_NAMES);
                String decoration = CommentConfig.getRandomElement(CommentConfig.NAME_DECORATIONS);
                return decoration + baseName + CommentConfig.getRandomElement(CommentConfig.NAME_DECORATIONS);
                
            case 2: // 网络昵称风格
                return CommentConfig.getRandomElement(CommentConfig.NAME_PREFIXES) + 
                       CommentConfig.getRandomElement(CommentConfig.NAME_SUFFIXES);
                
            case 3: // 带数字的用户名
                return CommentConfig.getRandomElement(CommentConfig.STANDARD_NAMES) + random.nextInt(1000);
                
            case 4: // 简短的英文名
                return CommentConfig.getRandomElement(CommentConfig.ENGLISH_NAMES);
                
            default:
                return CommentConfig.getRandomElement(CommentConfig.STANDARD_NAMES);
        }
    }
} 
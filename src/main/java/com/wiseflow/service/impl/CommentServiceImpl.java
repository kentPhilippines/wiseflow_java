package com.wiseflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wiseflow.entity.*;
import com.wiseflow.mapper.CommentMapper;
import com.wiseflow.mapper.DomainConfigMapper;
import com.wiseflow.service.CommentRuleService;
import com.wiseflow.service.CommentService;
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
    public void generateAiComments(Integer newsId, int count) {
     /*   // 获取文章信息
        News news = newsService.findById(newsId)
                .orElseThrow(() -> new RuntimeException("文章不存在: " + newsId));
        DomainConfig domainConfig = domainConfigMapper.selectByDomain(news.getDomainConfig());

        // 获取评论规则
        List<CommentRule> byDomainConfig = commentRuleService.findByDomainConfig(news.getDomainConfig());
        byDomainConfig.forEach(rule -> {
            if (rule == null || !rule.getEnabled() || !rule.getEnableAiComment()) {
                log.info("该域名未启用AI评论或评论规则不存在: {}", news.getDomainConfig());
                return;
            }

            // 获取可用的SEO关键词
            List<SeoKeyword> keywords;
            try {
                // 尝试将域名配置ID转换为Long类型
                Long domainConfigId = null;
                try {
                    // 如果domainConfig是数字ID，则直接转换
                    domainConfigId = Long.valueOf(domainConfig.getId());
                } catch (NumberFormatException e) {
                    // 如果domainConfig是域名字符串，则获取默认关键词
                    log.warn("域名格式不是有效的ID: {}, 将使用通用关键词", domainConfig.getId());
                    domainConfigId = 1L; // 使用默认ID，您可能需要调整为适合您系统的默认ID
                }

                keywords = seoKeywordService.findByUseScene(domainConfigId, 2);  // 使用场景为评论(2)

                // 如果找不到关键词，尝试使用两者都用的场景(3)
                if (keywords == null || keywords.isEmpty()) {
                    keywords = seoKeywordService.findByUseScene(domainConfigId, 3);  // 两者都用(3)
                }
            } catch (Exception e) {
                log.error("获取SEO关键词时出错: domainConfig={}, error={}",domainConfig.getId(), e.getMessage());
                keywords = new ArrayList<>(); // 使用空列表避免NPE
            }

            if (keywords.isEmpty()) {
                log.info("没有可用于评论的SEO关键词: domainConfig={}",domainConfig.getId());
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
        });*/
    }

    /**
     * 生成包含关键词的评论
     */
    private Comment generateCommentWithKeyword(News news, SeoKeyword keyword, CommentRule rule, boolean useAi) {


        Comment comment = new Comment();
        comment.setNewsId(news.getId());
        comment.setCommentTime(generateRandomCommentTime(news, rule));
        comment.setLikeCount(random.nextInt(10));  // 随机点赞数
        comment.setSeoKeywordId(keyword.getId());
        comment.setIsAiGenerated(true);
        comment.setDomainConfig(news.getDomainConfig());

        return comment;
    }

    /**
     * 生成普通评论
     */
    private Comment generateNormalComment(News news, CommentRule rule, boolean useAi) {
        // 决定使用AI还是模板生成评论
        String content;
        
        if (useAi) {
            try {
                String category = news.getCategoryName() != null ? news.getCategoryName() : "新闻";
                content = sportCommentAiUtil.generateSportComment(news.getTitle(), category);
            } catch (Exception e) {
                log.warn("AI生成普通评论失败，使用模板: {}", e.getMessage());
                // 从文章标题提取关键信息
                String title = news.getTitle();
                content = "好文章，" + (title.length() > 10 ? title.substring(0, 10) : title) + "写得真不错！";
            }
        } else {
            // 使用随机方式生成评论内容
            int randomType = random.nextInt(6); // 增加到6种不同的评论生成方式
            
            switch (randomType) {
                case 0: // 使用标准模板
                    String[] standardTemplates = {
                        "这篇文章很有见地，学到了不少东西。",
                        "感谢分享，内容很详细。",
                        "写得真好，继续关注。",
                        "点赞支持，期待更多内容。",
                        "分析得很到位，写得很专业。",
                        "不错的观点，值得思考。",
                        "标题很吸引人，内容也没有让人失望。",
                        "文章观点独到，很有启发性。",
                        "喜欢这种深度分析，内容充实。",
                        "内容有深度，不是一般的水文。",
                        "说得很有道理，我很赞同。",
                        "这个角度很新颖，没想到过。",
                        "逻辑清晰，论点有力。",
                        "总结得很到位，思路清晰。",
                        "赞同作者的看法，写得很好。"
                    };
                    content = standardTemplates[random.nextInt(standardTemplates.length)];
                    break;
                    
                case 1: // 引用文章标题的评论
                    String title = news.getTitle();
                    String[] titleTemplates = {
                        "《%s》真是一篇很棒的文章！",
                        "看完《%s》，收获很多。",
                        "《%s》这篇文章写得实在太好了。",
                        "「%s」确实说得很有道理。",
                        "看到《%s》这个标题就忍不住点进来了，内容也不错。",
                        "《%s》讲的问题正是我关心的。",
                        "《%s》这个话题很有意思，写得也很精彩。"
                    };
                    
                    if (title.length() > 15) {
                        title = title.substring(0, 15) + "...";
                    }
                    
                    content = String.format(titleTemplates[random.nextInt(titleTemplates.length)], title);
                    break;
                    
                case 2: // 情感型评论
                    String[] emotionalTemplates = {
                        "太喜欢这篇文章了！写得真好！",
                        "看完之后心情很愉悦，谢谢分享！",
                        "这种内容太赞了，每次都能学到新东西！",
                        "很高兴看到这样的好文章，收藏了！",
                        "哇，这篇太精彩了，忍不住一口气看完！",
                        "真是让人耳目一新的好文章！",
                        "看完让人振奋，思路开阔了很多！",
                        "每次看到这种高质量的文章都很开心！",
                        "写得太棒了，完全被吸引住了！",
                        "不得不说，这篇文章真是太棒了！",
                        "这就是我想看的优质内容！"
                    };
                    content = emotionalTemplates[random.nextInt(emotionalTemplates.length)];
                    break;
                    
                case 3: // 问题型评论
                    String[] questionTemplates = {
                        "文章写得很好，想请教一下作者对后续发展有什么看法？",
                        "内容很有启发性，不知道这个方法适用于所有情况吗？",
                        "有没有相关的延伸阅读推荐？这篇文章很有深度。",
                        "对于文中提到的观点，有没有其他案例可以借鉴？",
                        "能否分享更多这方面的内容？真的很喜欢这种风格。",
                        "请问作者对这个领域有什么独特见解吗？文章写得很专业。",
                        "这种方法实践起来难度大吗？看起来很有价值。",
                        "有没有入门级的建议给想学习这方面的新手？",
                        "能否多出一些这样的文章？内容很棒！"
                    };
                    content = questionTemplates[random.nextInt(questionTemplates.length)];
                    break;
                    
                case 4: // 组合型评论
                    String[] positiveWords = {"精彩", "出色", "优秀", "卓越", "专业", "深刻", "详细", "全面", "有见地", "有创见"};
                    String[] contentWords = {"分析", "讲解", "见解", "观点", "思路", "论证", "表达", "叙述", "总结", "归纳"};
                    String[] endings = {
                        "收藏了！", 
                        "学习了！", 
                        "获益匪浅！", 
                        "很有帮助！", 
                        "值得推荐！", 
                        "期待更多！", 
                        "感谢分享！",
                        "点赞支持！",
                        "下次还来看！",
                        "真的不错！"
                    };
                    
                    content = "文章的" + 
                              contentWords[random.nextInt(contentWords.length)] + 
                              "非常" + 
                              positiveWords[random.nextInt(positiveWords.length)] + 
                              "，" + 
                              endings[random.nextInt(endings.length)];
                    break;
                    
                case 5: // 带文章主题的评论
                    String category = news.getCategoryName();
                    if (category == null || category.trim().isEmpty()) {
                        category = "这个主题";
                    }
                    
                    String[] categoryTemplates = {
                        "作为%s的爱好者，这篇文章确实写得很专业。",
                        "难得看到关于%s的高质量文章，很棒！",
                        "对%s感兴趣的朋友一定要看这篇文章。",
                        "从%s的角度来看，这篇文章提供了很好的见解。",
                        "平时就关注%s相关内容，这篇文章水平很高。",
                        "终于看到一篇讲%s讲得这么清楚的文章了。",
                        "对于想了解%s的人来说，这是一篇不可多得的好文章。",
                        "%s领域需要更多这样的优质内容。"
                    };
                    
                    content = String.format(categoryTemplates[random.nextInt(categoryTemplates.length)], category);
                    break;
                    
                default:
                    content = "这篇文章内容很充实，值得一读！";
            }
            
            // 随机添加额外元素，增加多样性
            if (random.nextFloat() < 0.3) {
                String[] additions = {
                    "👍 ", 
                    "❤️ ", 
                    "收藏了！", 
                    "转发了！", 
                    "学习了！", 
                    "mark一下，以后再读！", 
                    "建议大家都来看看！", 
                    "值得细读！",
                    "期待更新！",
                    "支持原创！"
                };
                
                // 随机决定是添加在开头还是结尾
                if (random.nextBoolean()) {
                    content = additions[random.nextInt(additions.length)] + content;
                } else {
                    content = content + " " + additions[random.nextInt(additions.length)];
                }
            }
        }

        // 调整评论长度
        if (content.length() < rule.getMinCommentLength()) {
            content += "，" + generateRandomWords(rule.getMinCommentLength() - content.length());
        }

        if (content.length() > rule.getMaxCommentLength()) {
            content = content.substring(0, rule.getMaxCommentLength() - 3) + "...";
        }

        // 随机生成评论者名称
        String commenterName;
        int nameType = random.nextInt(5);
        
        switch (nameType) {
            case 0: // 标准名称
                commenterName = COMMENTER_NAMES[random.nextInt(COMMENTER_NAMES.length)];
                break;
                
            case 1: // 带特殊符号的名称
                String baseName = COMMENTER_NAMES[random.nextInt(COMMENTER_NAMES.length)];
                String[] decorations = {"✨", "🌟", "🔥", "💫", "⭐", "👑", "💯", "🌈", "🍀", "🌺"};
                commenterName = decorations[random.nextInt(decorations.length)] + baseName + decorations[random.nextInt(decorations.length)];
                break;
                
            case 2: // 网络昵称风格
                String[] prefixes = {"快乐的", "可爱的", "聪明的", "勇敢的", "悠闲的", "阳光的", "睿智的", "活力的", "安静的", "热情的"};
                String[] suffixes = {"读者", "旅行者", "思考者", "探索者", "学习者", "行者", "追梦人", "小天使", "守望者", "生活家"};
                commenterName = prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)];
                break;
                
            case 3: // 带数字的用户名
                commenterName = COMMENTER_NAMES[random.nextInt(COMMENTER_NAMES.length)] + random.nextInt(1000);
                break;
                
            case 4: // 简短的英文名
                String[] englishNames = {
                    "Alex", "Bob", "Cathy", "David", "Emma", "Frank", "Grace", "Henry", "Ivy", "Jack",
                    "Kate", "Leo", "Mary", "Nick", "Olivia", "Peter", "Queen", "Robin", "Sam", "Tina"
                };
                commenterName = englishNames[random.nextInt(englishNames.length)];
                break;
                
            default:
                commenterName = COMMENTER_NAMES[random.nextInt(COMMENTER_NAMES.length)];
        }

        Comment comment = new Comment();
        comment.setNewsId(news.getId());
        comment.setContent(content);
        comment.setCommenterName(commenterName);
        comment.setCommentTime(generateRandomCommentTime(news, rule));
        comment.setLikeCount(random.nextInt(10));  // 随机点赞数
        comment.setIsAiGenerated(true);
        comment.setDomainConfig(news.getDomainConfig());

        return comment;
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
        String[] fillers = {
                "感谢分享", "学习了", "收藏了", "点赞支持", "期待更新",
                "内容很丰富", "讲解很清晰", "很有帮助", "写得太好了", "继续关注"
        };

        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            if (sb.length() > 0) {
                sb.append("，");
            }
            sb.append(fillers[random.nextInt(fillers.length)]);
        }

        return sb.toString();
    }
} 
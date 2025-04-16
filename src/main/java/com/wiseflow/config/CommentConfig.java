package com.wiseflow.config;

import java.util.Random;

/**
 * 评论相关配置常量
 */
public class CommentConfig {
    
    private static final Random random = new Random();

    /**
     * 标准用户名
     */
    public static final String[] STANDARD_NAMES = {
        "张三", "李四", "王五", "赵六", "田七", "周八", "吴九", "郑十",
        "小明", "小红", "小刚", "小丽", "小华", "小林", "小雪", "小梅",
        "阳光", "明月", "繁星", "流云", "清风", "细雨", "春风", "夏雨"
    };

    /**
     * 英文名
     */
    public static final String[] ENGLISH_NAMES = {
        "Alex", "Bob", "Cathy", "David", "Emma", "Frank", "Grace", "Henry", "Ivy", "Jack",
        "Kate", "Leo", "Mary", "Nick", "Olivia", "Peter", "Queen", "Robin", "Sam", "Tina"
    };

    /**
     * 用户名前缀
     */
    public static final String[] NAME_PREFIXES = {
        "快乐的", "可爱的", "聪明的", "勇敢的", "悠闲的", "阳光的", "睿智的", "活力的", "安静的", "热情的"
    };

    /**
     * 用户名后缀
     */
    public static final String[] NAME_SUFFIXES = {
        "读者", "旅行者", "思考者", "探索者", "学习者", "行者", "追梦人", "小天使", "守望者", "生活家"
    };

    /**
     * 用户名装饰符
     */
    public static final String[] NAME_DECORATIONS = {
        "✨", "🌟", "🔥", "💫", "⭐", "👑", "💯", "🌈", "🍀", "🌺"
    };

    /**
     * 作者评论模板
     */
    public static final String[] AUTHOR_COMMENT_TEMPLATES = {
        "%s老师的文章一如既往地专业",
        "又见到%s的文章了，每次都有新收获",
        "最近经常看%s的文章，写得都很好",
        "非常喜欢%s的写作风格，很有见地",
        "感谢%s的分享，文章写得很用心",
        "%s的文章总是能带来新的思考",
        "一看就知道是%s写的，风格很独特",
        "期待%s更多的好文章",
        "最近%s的文章质量都很高",
        "认真拜读了%s的大作，受益匪浅"
    };

    /**
     * 内容关键词评论模板
     */
    public static final String[] CONTENT_KEYWORD_TEMPLATES = {
        "关于%s的分析很到位，学到了很多",
        "对%s这个话题一直很感兴趣，写得很专业",
        "文章对%s的见解很独到，值得深入思考",
        "难得看到讲%s讲得这么清楚的文章",
        "这篇文章把%s的问题讲得很透彻",
        "看完对%s有了更深的理解",
        "文章中关于%s的部分特别精彩",
        "第一次看到从这个角度解读%s的文章",
        "对%s感兴趣的朋友不要错过这篇文章",
        "%s相关的好文章，收藏了"
    };

    /**
     * 标准评论模板
     */
    public static final String[] STANDARD_COMMENT_TEMPLATES = {
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

    /**
     * 标题评论模板
     */
    public static final String[] TITLE_COMMENT_TEMPLATES = {
        "《%s》真是一篇很棒的文章！",
        "看完《%s》，收获很多。",
        "《%s》这篇文章写得实在太好了。",
        "「%s」确实说得很有道理。",
        "看到《%s》这个标题就忍不住点进来了，内容也不错。",
        "《%s》讲的问题正是我关心的。",
        "《%s》这个话题很有意思，写得也很精彩。"
    };

    /**
     * 情感评论模板
     */
    public static final String[] EMOTIONAL_COMMENT_TEMPLATES = {
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

    /**
     * 问题评论模板
     */
    public static final String[] QUESTION_COMMENT_TEMPLATES = {
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

    /**
     * 评论补充语
     */
    public static final String[] COMMENT_ADDITIONS = {
        "👍 ", "❤️ ", "收藏了！", "转发了！", "学习了！", 
        "mark一下，以后再读！", "建议大家都来看看！", "值得细读！",
        "期待更新！", "支持原创！"
    };

    /**
     * 评论填充语
     */
    public static final String[] COMMENT_FILLERS = {
        "感谢分享", "学习了", "收藏了", "点赞支持", "期待更新",
        "内容很丰富", "讲解很清晰", "很有帮助", "写得太好了", "继续关注"
    };

    /**
     * 关键词插入短语
     */
    public static final String[] KEYWORD_INSERT_PHRASES = {
        "说到%s，",
        "关于%s，",
        "谈到%s，",
        "对于%s，",
        "就像%s一样，",
        "正如%s所说的，"
    };

    /**
     * 获取随机元素
     */
    public static String getRandomElement(String[] array) {
        return array[random.nextInt(array.length)];
    }
} 
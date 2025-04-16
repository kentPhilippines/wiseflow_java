CREATE TABLE `wf_article_rewrite` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `original_article_id` bigint(20) NOT NULL COMMENT '原文章ID',
    `title` varchar(255) NOT NULL COMMENT '改写后的标题',
    `content` text NOT NULL COMMENT '改写后的内容',
    `originality_score` int(11) DEFAULT NULL COMMENT '原创度评分（0-100）',
    `status` varchar(20) NOT NULL COMMENT '处理状态',
    `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
    `create_time` datetime NOT NULL COMMENT '创建时间',
    `update_time` datetime NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_original_article_id` (`original_article_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章改写记录表'; 
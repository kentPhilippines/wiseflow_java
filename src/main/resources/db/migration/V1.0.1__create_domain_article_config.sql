CREATE TABLE `domain_article_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `domain_id` bigint(20) NOT NULL COMMENT '域名ID',
  `type_id` bigint(20) NOT NULL COMMENT '文章类型ID',
  `min_count` int(11) NOT NULL DEFAULT '0' COMMENT '最小文章数',
  `max_count` int(11) NOT NULL DEFAULT '0' COMMENT '最大文章数',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_type` (`domain_id`, `type_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域名文章类型配置表'; 
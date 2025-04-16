CREATE TABLE `article_rule` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `type_id` bigint(20) NOT NULL COMMENT '文章类型ID',
  `domain_id` bigint(20) NOT NULL COMMENT '域名ID',
  `min_count` int(11) NOT NULL DEFAULT '0' COMMENT '最小文章数',
  `max_count` int(11) NOT NULL DEFAULT '0' COMMENT '最大文章数',
  `rule_config` text NOT NULL COMMENT '配置规则JSON字符串',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用：0-禁用，1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_domain` (`type_id`, `domain_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分配规则表'; 
-- 为域名配置表添加服务器编号字段
-- V1.0.5__add_server_info_to_domain_config.sql

-- 添加服务器编号字段
ALTER TABLE wf_domain_config ADD COLUMN server_info VARCHAR(500) COMMENT '服务器编号/备注';

-- 创建索引以提升查询性能
CREATE INDEX idx_wf_domain_config_server_info ON wf_domain_config(server_info);

-- 更新表注释
ALTER TABLE wf_domain_config COMMENT = '域名配置表 - 存储网站的TDK（标题、描述、关键词）、友情链接和服务器信息等配置'; 
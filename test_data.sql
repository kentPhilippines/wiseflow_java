-- 测试数据：验证三级展示功能（服务器 -> 批次 -> 域名）

-- 服务器A下的两个批次
INSERT INTO wf_domain_config (domain, title, description, keywords, server_info, contact_phone, status, create_time) VALUES
('example1.com', '测试网站1', '这是测试描述1', '关键词1,关键词2', '服务器A', '13800001001', 1, NOW()),
('example2.com', '测试网站2', '这是测试描述2', '关键词3,关键词4', '服务器A', '13800001001', 1, NOW()),
('example3.com', '测试网站3', '这是测试描述3', '关键词5,关键词6', '服务器A', '13800002002', 0, NOW()),
('example4.com', '测试网站4', '这是测试描述4', '关键词7,关键词8', '服务器A', '13800002002', 1, NOW());

-- 服务器B下的一个批次
INSERT INTO wf_domain_config (domain, title, description, keywords, server_info, contact_phone, status, create_time) VALUES
('example5.com', '测试网站5', '这是测试描述5', '关键词9,关键词10', '服务器B', '13800003003', 1, NOW()),
('example6.com', '测试网站6', '这是测试描述6', '关键词11,关键词12', '服务器B', '13800003003', 0, NOW());

-- 未设置服务器的域名
INSERT INTO wf_domain_config (domain, title, description, keywords, server_info, contact_phone, status, create_time) VALUES
('example7.com', '测试网站7', '这是测试描述7', '关键词13,关键词14', NULL, '13800004004', 1, NOW()),
('example8.com', '测试网站8', '这是测试描述8', '关键词15,关键词16', NULL, '13800004004', 1, NOW());

-- 预期的三级展示结构：
-- 1. 服务器A (4个域名，2个批次)
--    ├── 批次 13800001001 (2个域名，2个启用)
--    │   ├── example1.com (启用)
--    │   └── example2.com (启用)
--    └── 批次 13800002002 (2个域名，1个启用，1个禁用)
--        ├── example3.com (禁用)
--        └── example4.com (启用)
--
-- 2. 服务器B (2个域名，1个批次)
--    └── 批次 13800003003 (2个域名，1个启用，1个禁用)
--        ├── example5.com (启用)
--        └── example6.com (禁用)
--
-- 3. 未设置服务器 (2个域名，1个批次)
--    └── 批次 13800004004 (2个域名，2个启用)
--        ├── example7.com (启用)
--        └── example8.com (启用) 
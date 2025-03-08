package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.DomainConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 域名配置数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface DomainConfigMapper extends BaseMapper<DomainConfig> {
    
    /**
     * 根据域名查询配置
     */
    @Select("SELECT * FROM wf_domain_config WHERE domain = #{domain} AND status = 1 LIMIT 1")
    DomainConfig selectByDomain(@Param("domain") String domain);
    
    /**
     * 检查域名是否存在
     */
    @Select("SELECT COUNT(*) FROM wf_domain_config WHERE domain = #{domain}")
    int countByDomain(@Param("domain") String domain);
} 
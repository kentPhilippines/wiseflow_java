package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.CommentRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 评论规则数据访问接口
 */
@Mapper
public interface CommentRuleMapper extends BaseMapper<CommentRule> {
    
    /**
     * 根据域名查询评论规则
     */
    @Select("SELECT * FROM wf_comment_rule WHERE domain_config = #{domainConfig}")
    List<CommentRule> findByDomainConfig(@Param("domainConfig") String domainConfig);
    
    /**
     * 更新规则状态
     */
    @Update("UPDATE wf_comment_rule SET enabled = #{enabled} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("enabled") Boolean enabled);
    
    /**
     * 检查域名是否已配置评论规则
     */
    @Select("SELECT COUNT(*) FROM wf_comment_rule WHERE domain_config = #{domainConfig}")
    int countByDomainConfig(@Param("domainConfig") String domainConfig);
} 
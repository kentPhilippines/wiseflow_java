package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wiseflow.entity.FailedUrl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 失败URL数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface FailedUrlMapper extends BaseMapper<FailedUrl> {
    
    /**
     * 根据爬虫名称查询失败URL
     */
    @Select("SELECT * FROM wf_failed_url WHERE spider_name = #{spiderName} ORDER BY create_time DESC")
    List<FailedUrl> selectBySpiderName(@Param("spiderName") String spiderName);
    
    /**
     * 根据状态查询失败URL
     */
    @Select("SELECT * FROM wf_failed_url WHERE status = #{status} ORDER BY create_time DESC")
    List<FailedUrl> selectByStatus(@Param("status") Integer status);
    
    /**
     * 分页查询所有失败URL
     */
    @Select("SELECT * FROM wf_failed_url ORDER BY create_time DESC")
    IPage<FailedUrl> selectAllPaged(Page<FailedUrl> page);
} 
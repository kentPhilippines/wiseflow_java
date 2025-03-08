package com.wiseflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wiseflow.entity.CrawlLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 爬虫日志数据访问接口
 * 使用MyBatis-Plus实现，不需要事务管理
 */
@Mapper
public interface CrawlLogMapper extends BaseMapper<CrawlLog> {
    
    /**
     * 根据爬虫名称查询日志
     */
    @Select("SELECT * FROM wf_crawl_log WHERE spider_name = #{spiderName} ORDER BY start_time DESC")
    List<CrawlLog> selectBySpiderName(@Param("spiderName") String spiderName);
    
    /**
     * 根据状态查询日志
     */
    @Select("SELECT * FROM wf_crawl_log WHERE status = #{status} ORDER BY start_time DESC")
    List<CrawlLog> selectByStatus(@Param("status") Integer status);
    
    /**
     * 根据时间范围查询日志
     */
    @Select("SELECT * FROM wf_crawl_log WHERE start_time BETWEEN #{startTime} AND #{endTime} ORDER BY start_time DESC")
    List<CrawlLog> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取爬虫成功率统计
     */
    @Select("SELECT spider_name, COUNT(*) as total, SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as success_count " +
            "FROM wf_crawl_log GROUP BY spider_name")
    List<Object[]> getSpiderSuccessRateStats();
} 
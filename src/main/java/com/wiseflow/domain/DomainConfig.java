package com.wiseflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName("wf_domain_config")
public class DomainConfig {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 域名
     */
    private String domain;
    
    /**
     * 主IP地址
     */
    private String mainIp;
    
    /**
     * 网站标题
     */
    private String title;
    
    /**
     * 网站描述
     */
    private String description;
    
    /**
     * 网站关键词
     */
    private String keywords;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * Logo URL
     */
    private String logoUrl;
    
    /**
     * Favicon URL
     */
    private String faviconUrl;
    
    /**
     * 版权信息
     */
    private String copyright;
    
    /**
     * 备案号
     */
    private String icp;
    
    /**
     * 模板路径
     */
    private String viewsPath;
    
    /**
     * 联系电话
     */
    private String contactPhone;
    
    /**
     * 联系邮箱
     */
    private String contactEmail;
    
    /**
     * 联系地址
     */
    private String contactAddress;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 每日新增文章数量
     */
    private Integer dailyAddNewsCount;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    /**
     * 评论规则ID列表
     */
    @TableField(exist = false)
    private List<Integer> commentRuleIds;
    
    /**
     * 改写规则ID列表
     */
    @TableField(exist = false)
    private List<Integer> rewriteRuleIds;
    
    /**
     * 文章分配规则ID
     */
    @TableField(exist = false)
    private Integer articleRuleId;
} 
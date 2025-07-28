package com.wiseflow.vo;

import lombok.Data;

/**
 * API统一返回结果类
 */
@Data
public class ApiResult {
    
    /**
     * 响应码，200表示成功
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 返回数据
     */
    private Object data;
    
    /**
     * 创建成功结果
     */
    public static ApiResult success() {
        ApiResult result = new ApiResult();
        result.setCode(200);
        result.setSuccess(true);
        result.setMessage("操作成功");
        return result;
    }
    
    /**
     * 创建成功结果（带数据）
     */
    public static ApiResult success(Object data) {
        ApiResult result = success();
        result.setData(data);
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static ApiResult fail(String message) {
        ApiResult result = new ApiResult();
        result.setCode(500);
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
    
    /**
     * 创建失败结果（带错误码）
     */
    public static ApiResult fail(int code, String message) {
        ApiResult result = new ApiResult();
        result.setCode(code);
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
} 
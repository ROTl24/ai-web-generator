package com.uloaix.xiaolu_aicode.common;

import java.io.Serializable;

import com.uloaix.xiaolu_aicode.exception.ErrorCode;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用响应体
 * 包含响应码、响应数据、响应消息
 * 用于封装接口的返回值
 */
@Data
@NoArgsConstructor
public class BaseResponse<T> implements Serializable {

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 构造函数
     *
     * @param code 响应码
     * @param data 响应数据
     * @param message 响应消息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 构造函数
     *
     * @param code 响应码
     * @param data 响应数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 构造函数
     *
     * @param errorCode 错误码
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

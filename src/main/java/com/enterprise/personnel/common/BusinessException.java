package com.enterprise.personnel.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器转换为统一响应
 */
@Getter
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

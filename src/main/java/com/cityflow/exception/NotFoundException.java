package com.cityflow.exception;

import com.cityflow.dto.ErrorCode;

/**
 * 资源不存在专用异常——比 BizException 更具语义。
 *
 * 用法：
 *   throw new NotFoundException(ErrorCode.VOUCHER_NOT_FOUND, "voucherId=" + id);
 */
public class NotFoundException extends BizException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotFoundException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}

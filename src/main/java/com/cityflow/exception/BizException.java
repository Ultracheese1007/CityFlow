package com.cityflow.exception;

import com.cityflow.dto.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类——所有可预期的业务错误都通过它（或其子类）抛出。
 *
 * 跟普通 RuntimeException 的区别：
 *   - 携带 ErrorCode（决定 HTTP 状态码 + 稳定的 API code 字符串）
 *   - WebExceptionAdvice 会把它转成结构化的 Result 响应，带正确的 HTTP code
 *   - 不会走"500 服务器异常"兜底分支
 *
 * 用法：
 *   throw new BizException(ErrorCode.VOUCHER_NOT_FOUND);
 *   throw new BizException(ErrorCode.STOCK_INSUFFICIENT, "voucherId=" + id);
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}

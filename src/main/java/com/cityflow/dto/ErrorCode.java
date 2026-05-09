package com.cityflow.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务错误码定义。
 *
 * 设计原则：
 *   - code: 给前端 / API consumer 用的稳定字符串（永远不变，可以做 i18n 查表 key）
 *   - httpStatus: 这个错该返回什么 HTTP 状态码，由 WebExceptionAdvice 拿出来用
 *   - defaultMessage: 给人看的默认中文文案（service 层可以用 fail(code, customMsg) 覆盖）
 *
 * 添加新错误码时按"领域 + 行为"分组（如 USER_xxx / VOUCHER_xxx）。
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ---- 通用 ----
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "服务器异常"),
    INVALID_REQUEST(400, "INVALID_REQUEST", "请求参数错误"),
    NOT_FOUND(404, "NOT_FOUND", "资源不存在"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录"),

    // ---- 用户 / 认证 ----
    INVALID_PHONE(400, "INVALID_PHONE", "手机号格式错误"),
    INVALID_CODE(400, "INVALID_CODE", "验证码错误"),

    // ---- 秒杀 ----
    VOUCHER_NOT_FOUND(404, "VOUCHER_NOT_FOUND", "秒杀券不存在"),
    VOUCHER_NOT_STARTED(400, "VOUCHER_NOT_STARTED", "秒杀券尚未开始"),
    VOUCHER_ENDED(400, "VOUCHER_ENDED", "秒杀券已结束"),
    STOCK_INSUFFICIENT(400, "STOCK_INSUFFICIENT", "库存不足"),
    ALREADY_PURCHASED(400, "ALREADY_PURCHASED", "用户已经购买过一次"),
    CONCURRENT_ORDER_DENIED(429, "CONCURRENT_ORDER_DENIED", "请求过于频繁，请稍后重试");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;
}

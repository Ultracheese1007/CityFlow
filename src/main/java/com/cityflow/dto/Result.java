package com.cityflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一响应封装。
 *
 * 字段说明：
 *   - success: 布尔，前端最快的判断字段
 *   - code: 错误码（仅失败时有；成功时 null 不序列化）
 *   - errorMsg: 给人看的错误描述（仅失败时有）
 *   - data: 业务数据
 *   - total: 分页用的总数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result {
    private Boolean success;
    private String code;          // 新增字段：稳定错误码，如 "VOUCHER_NOT_FOUND"
    private String errorMsg;
    private Object data;
    private Long total;

    public static Result ok() {
        return new Result(true, null, null, null, null);
    }

    public static Result ok(Object data) {
        return new Result(true, null, null, data, null);
    }

    public static Result ok(List<?> data, Long total) {
        return new Result(true, null, null, data, total);
    }

    /**
     * @deprecated 旧式 fail——没有错误码，仅保留兼容现有调用点。
     *             新代码请用 Result.fail(ErrorCode) 或 Result.fail(ErrorCode, customMsg)。
     */
    @Deprecated
    public static Result fail(String errorMsg) {
        return new Result(false, null, errorMsg, null, null);
    }

    /** 推荐：用错误码生成 fail，errorMsg 用错误码自带的默认文案 */
    public static Result fail(ErrorCode error) {
        return new Result(false, error.getCode(), error.getDefaultMessage(), null, null);
    }

    /** 推荐：用错误码 + 自定义文案（e.g. 拼上具体的 ID） */
    public static Result fail(ErrorCode error, String customMessage) {
        return new Result(false, error.getCode(), customMessage, null, null);
    }
}

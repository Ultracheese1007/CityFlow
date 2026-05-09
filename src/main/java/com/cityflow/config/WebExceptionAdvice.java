package com.cityflow.config;

import com.cityflow.dto.ErrorCode;
import com.cityflow.dto.Result;
import com.cityflow.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * 处理顺序（具体在前、兜底在后）：
 *   1. BizException        → 200 (带错误码 JSON)，业务可预期错误
 *   2. MethodArgumentNotValid → 400，参数校验失败
 *   3. Exception           → 500，未知异常兜底
 *
 * 设计取舍：
 *   - BizException 仍然返回 HTTP 200——因为业务"失败"不是 HTTP 协议层面的失败。
 *     errorCode 信息走 body 里的 result.code 字段，前端按 success 字段分支处理。
 *     这跟很多国内电商 API 风格一致；如果你倾向 RESTful 严格语义，
 *     可以改用 errorCode.getHttpStatus() 作为响应状态码（注释里有示例）。
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 业务异常：日志按 warn 级别（不是 error），因为是预期内的失败
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result> handleBizException(BizException e) {
        ErrorCode err = e.getErrorCode();
        log.warn("Business exception: code={} msg={}", err.getCode(), e.getMessage());

        // 走 200 + body 里带 code 的风格（保持跟现有 API 兼容）
        return ResponseEntity.ok(Result.fail(err, e.getMessage()));

        // 如果想走 RESTful 严格状态码风格（比如 404 真的返回 404）：
        // return ResponseEntity.status(err.getHttpStatus()).body(Result.fail(err, e.getMessage()));
    }

    /**
     * 参数校验失败（@Valid + @RequestBody / @RequestParam 等）
     * 把所有字段错误拼成一句人话返回
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(ErrorCode.INVALID_REQUEST, details));
    }

    /**
     * 兜底：所有未被前面处理掉的异常——记 error 日志，返回 500
     * 这条意味着一个 bug，需要立刻看日志
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);    // 完整 stack trace
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ErrorCode.INTERNAL_ERROR));
    }
}

package com.cityflow.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 秒杀订单创建事件——Redis 校验通过后由 producer 发出，
 * consumer 收到后异步把订单写入 MySQL。
 *
 * 序列化为 JSON，topic key = voucherId.toString()
 * （同一个商品的所有订单事件进同一 partition，保证顺序）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /** 订单 ID（HTTP 响应里返回给用户的就是这个） */
    private Long orderId;

    /** 下单的用户 ID */
    private Long userId;

    /** 抢的优惠券 ID */
    private Long voucherId;

    /** 事件发生时间——便于 consumer 处理时排查 / 监控延迟 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

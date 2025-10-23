package com.hmdp.dto;

import java.time.LocalDateTime;

public interface VoucherView {
    Long getId();
    Long getShopId();
    String getTitle();
    String getSubTitle();
    String getRules();
    Long getPayValue();
    Long getActualValue();
    Integer getType();
    Integer getStatus();
    Integer getStock();
    LocalDateTime getBeginTime();
    LocalDateTime getEndTime();
    LocalDateTime getCreateTime();
    LocalDateTime getUpdateTime();
}

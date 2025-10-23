package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

import java.util.List;

public interface ShopService {
    Result queryById(Long id);
    Long saveShop(Shop shop);
    boolean updateShop(Shop shop);
    List<Shop> queryByType(Long typeId, Integer current);
    List<Shop> queryByName(String name, Integer current);
}
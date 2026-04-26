package com.cityflow.service;

import com.cityflow.dto.Result;
import com.cityflow.entity.Shop;

import java.util.List;

public interface ShopService {
    Result queryById(Long id);
    Long saveShop(Shop shop);
    boolean updateShop(Shop shop);
    List<Shop> queryByType(Long typeId, Integer current);
    List<Shop> queryByName(String name, Integer current);
}
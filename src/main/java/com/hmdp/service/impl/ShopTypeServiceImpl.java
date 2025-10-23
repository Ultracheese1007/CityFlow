package com.hmdp.service.impl;

import com.hmdp.entity.ShopType;
import com.hmdp.repository.ShopTypeRepository;
import com.hmdp.service.ShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopTypeServiceImpl implements ShopTypeService {

    private final ShopTypeRepository shopTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ShopType> queryTypeList() {
        // 按 sort 升序查询所有店铺类型
        return shopTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "sort"));
    }
}
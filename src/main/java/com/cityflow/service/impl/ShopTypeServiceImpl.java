package com.cityflow.service.impl;

import com.cityflow.entity.ShopType;
import com.cityflow.repository.ShopTypeRepository;
import com.cityflow.service.ShopTypeService;
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
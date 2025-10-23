package com.hmdp.web;

import com.hmdp.dto.Result;
import com.hmdp.service.ShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shop-type")
@RequiredArgsConstructor
public class ShopTypeController {

    private final ShopTypeService shopTypeService;

    @GetMapping("/list")
    public Result queryTypeList() {
        return Result.ok(shopTypeService.queryTypeList());
    }
}
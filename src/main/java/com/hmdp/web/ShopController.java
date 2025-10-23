package com.hmdp.web;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    /**
     * 根据id查询商铺信息
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        Long id = shopService.saveShop(shop);
        return Result.ok(id);
    }

    /**
     * 更新商铺信息
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        boolean ok = shopService.updateShop(shop);
        return ok ? Result.ok() : Result.fail("id is required");
    }

    /**
     * 根据商铺类型分页查询
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Long typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return Result.ok(shopService.queryByType(typeId, current));
    }

    /**
     * 根据名称关键字分页查询
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return Result.ok(shopService.queryByName(name, current));
    }
}
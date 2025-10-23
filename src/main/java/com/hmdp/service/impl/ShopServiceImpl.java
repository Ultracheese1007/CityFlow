package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.repository.ShopRepository;
import com.hmdp.service.ShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static com.hmdp.utils.RedisConstants.*;




@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;

    @Transactional(readOnly = true)
    public Shop getById(Long id) {
        return shopRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Result queryById(Long id) {
        // 解决缓存穿透
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 7.返回
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Long saveShop(Shop shop) {
        Shop saved = shopRepository.save(shop);
        return saved.getId();
    }

    @Override
    @Transactional
    public boolean updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return false;
        }
        // 更新数据库
        shopRepository.save(shop);
        // 删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> queryByType(Long typeId, Integer current) {
        int pageIndex = Math.max(0, (current == null ? 1 : current) - 1);
        Pageable pageable = PageRequest.of(pageIndex, SystemConstants.DEFAULT_PAGE_SIZE);
        return shopRepository.findByTypeId(typeId, pageable).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Shop> queryByName(String name, Integer current) {
        int pageIndex = Math.max(0, (current == null ? 1 : current) - 1);
        Pageable pageable = PageRequest.of(pageIndex, SystemConstants.MAX_PAGE_SIZE);
        if (name == null || name.isBlank()) {
            return shopRepository.findAll(pageable).getContent();
        }
        return shopRepository.findByNameContainingIgnoreCase(name, pageable).getContent();
    }
}
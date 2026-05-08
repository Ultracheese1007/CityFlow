package com.cityflow.service.impl;

import com.cityflow.dto.Result;
import com.cityflow.entity.Shop;
import com.cityflow.repository.ShopRepository;
import com.cityflow.utils.CacheClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ShopServiceImpl 单元测试
 *
 * 重点不是测 Redis cache-aside 模式（那是 CacheClient 自己的事），
 * 而是测 ShopServiceImpl 是否正确地：
 *   - 通过 CacheClient 读取（不 bypass 缓存）
 *   - 在 update 时同时写库 + 清缓存（一致性契约）
 *   - 在 id 不合法时拒绝写入（守门员）
 */
@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @Mock
    private ShopRepository shopRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CacheClient cacheClient;

    @InjectMocks
    private ShopServiceImpl shopService;

    @Test
    @DisplayName("queryById：cacheClient 返回 Shop 时包装成 Result.ok")
    void queryById_cacheHit_returnsOk() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("103茶餐厅");
        // CacheClient 返回 shop 对象
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(1L), eq(Shop.class), any(Function.class), anyLong(), any(TimeUnit.class)
        )).thenReturn(shop);

        Result result = shopService.queryById(1L);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(shop);
    }

    @Test
    @DisplayName("queryById：cacheClient 返回 null 时返回 fail")
    void queryById_cacheReturnsNull_returnsFail() {
        when(cacheClient.queryWithPassThrough(
                anyString(), eq(999L), eq(Shop.class), any(Function.class), anyLong(), any(TimeUnit.class)
        )).thenReturn(null);

        Result result = shopService.queryById(999L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("店铺不存在！");
    }

    @Test
    @DisplayName("saveShop：把 repository 返回的 id 透传出去")
    void saveShop_returnsGeneratedId() {
        Shop input = new Shop();
        input.setName("新店铺");

        Shop saved = new Shop();
        saved.setId(42L);
        saved.setName("新店铺");
        when(shopRepository.save(input)).thenReturn(saved);

        Long id = shopService.saveShop(input);

        assertThat(id).isEqualTo(42L);
        verify(shopRepository, times(1)).save(input);
    }

    @Test
    @DisplayName("updateShop：id 为 null 时拒绝更新，不写库不清缓存")
    void updateShop_nullId_returnsFalseAndNoOp() {
        Shop shop = new Shop();
        shop.setId(null);  // 故意构造非法输入
        shop.setName("脏数据");

        boolean result = shopService.updateShop(shop);

        assertThat(result).isFalse();
        // 关键：守门员行为——一行 db 写也不能发生
        verifyNoInteractions(shopRepository);
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("updateShop：合法更新时同时写库 + 清缓存（一致性契约）")
    void updateShop_validId_savesAndEvictsCache() {
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("更新后的店名");

        boolean result = shopService.updateShop(shop);

        assertThat(result).isTrue();
        // 1. 验证写库
        verify(shopRepository, times(1)).save(shop);
        // 2. 验证清缓存——key 必须是 "cache:shop:1"
        verify(stringRedisTemplate, times(1)).delete("cache:shop:1");
    }
}

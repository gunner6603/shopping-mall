package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.item.domain.entity.Product;
import com.gugucon.shopping.item.dto.response.ProductIds;
import com.gugucon.shopping.item.repository.ProductRepository;
import com.gugucon.shopping.utils.DomainUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {ProductCache.class})
@EnableCaching
@AutoConfigureCache(cacheProvider = CacheType.SIMPLE)
class ProductCacheTest {

    @Autowired
    ProductCache productCache;

    @MockBean
    ProductRepository productRepository;

    @AfterEach
    void tearDown() {
        productCache.clearAll();
    }

    @Test
    @DisplayName("메서드를 2번 연속 호출하면 실제 메서드는 1번만 실행된다.")
    void getRecommendationIds_invokeTwiceConsecutively() {
        // given
        final long productId = 10000L;
        final Pageable pageable = Pageable.ofSize(30).withPage(0);
        final Product productA = DomainUtils.createProduct("productA", 10000L);
        final Product productB = DomainUtils.createProduct("productB", 10000L);
        final List<Long> expectedProductIds = List.of(productA.getId(), productB.getId());

        when(productRepository.findRecommendedProducts(productId, pageable))
                .thenReturn(new SliceImpl<>(List.of(productA, productB), pageable, false));

        // when
        final ProductIds firstResult = productCache.getRecommendationIds(productId);
        final ProductIds secondResult = productCache.getRecommendationIds(productId);

        // then
        verify(productRepository, times(1)).findRecommendedProducts(productId, pageable);
        assertThat(firstResult).extracting(ProductIds::getContents)
                .isEqualTo(expectedProductIds);
        assertThat(secondResult).extracting(ProductIds::getContents)
                .isEqualTo(expectedProductIds);
    }

    @Test
    @DisplayName("모든 캐시 내용을 삭제한다.")
    void clearAll() {
        // given
        final long productId = 10000L;
        final Pageable pageable = Pageable.ofSize(30).withPage(0);
        final Product productA = DomainUtils.createProduct("productA", 10000L);
        final Product productB = DomainUtils.createProduct("productB", 10000L);
        final List<Long> expectedProductIds = List.of(productA.getId(), productB.getId());

        when(productRepository.findRecommendedProducts(productId, pageable))
                .thenReturn(new SliceImpl<>(List.of(productA, productB), pageable, false));

        // when
        final ProductIds firstResult = productCache.getRecommendationIds(productId);
        productCache.clearAll();
        final ProductIds secondResult = productCache.getRecommendationIds(productId);

        // then
        verify(productRepository, times(2)).findRecommendedProducts(productId, pageable);
        assertThat(firstResult).extracting(ProductIds::getContents)
                .isEqualTo(expectedProductIds);
        assertThat(secondResult).extracting(ProductIds::getContents)
                .isEqualTo(expectedProductIds);
    }

    @Test
    @DisplayName("캐시 내용을 갱신한다.")
    void setRecommendationIds() {
        final long productId = 10000L;
        final Pageable pageable = Pageable.ofSize(30).withPage(0);
        final Product productA = DomainUtils.createProduct("productA", 10000L);
        final Product productB = DomainUtils.createProduct("productB", 10000L);
        final List<Long> oldProductIds = List.of(productA.getId(), productB.getId());
        final List<Long> newProductIds = List.of(1234567L, 7654321L, 1234321L);

        when(productRepository.findRecommendedProducts(productId, pageable))
                .thenReturn(new SliceImpl<>(List.of(productA, productB), pageable, false));

        // when
        final ProductIds firstResult = productCache.getRecommendationIds(productId);
        productCache.setRecommendationIds(productId, newProductIds);
        final ProductIds secondResult = productCache.getRecommendationIds(productId);

        // then
        verify(productRepository, times(1)).findRecommendedProducts(productId, pageable);
        assertThat(firstResult).extracting(ProductIds::getContents)
                .isEqualTo(oldProductIds);
        assertThat(secondResult).extracting(ProductIds::getContents)
                .isEqualTo(newProductIds);
    }
}

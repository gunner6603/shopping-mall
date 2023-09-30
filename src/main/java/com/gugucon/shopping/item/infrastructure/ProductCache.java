package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.item.domain.entity.Product;
import com.gugucon.shopping.item.dto.response.ProductIds;
import com.gugucon.shopping.item.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCache {

    public static final int RECOMMENDATION_SIZE = 30;

    private final ProductRepository productRepository;

    @Cacheable(cacheNames = "rec", key = "#productId", sync = true)
    public ProductIds getRecommendationIds(final Long productId) {
        log.info("cache method invoked");
        final Pageable pageable = Pageable.ofSize(RECOMMENDATION_SIZE)
                .withPage(0);
        final List<Long> recommendationIds = productRepository.findRecommendedProducts(productId, pageable)
                .map(Product::getId)
                .toList();
        return ProductIds.from(recommendationIds);
    }

    @CachePut(cacheNames = "rec", key = "#productId")
    public ProductIds setRecommendationIds(final Long productId, final List<Long> recommendationProductIds) {
        return ProductIds.from(recommendationProductIds);
    }

    @CacheEvict(cacheNames = "rec", allEntries = true)
    public void clearAll() {
        log.info("cache eviction invoked");
    }
}

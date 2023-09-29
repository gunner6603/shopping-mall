package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.item.dto.response.ProductDetailResponse;
import com.gugucon.shopping.item.dto.response.ProductDetailResponses;
import com.gugucon.shopping.item.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Cacheable(cacheNames = "recommendation", key = "#productId")
    public ProductDetailResponses getRecommendations(final Long productId) {
        log.info("cache method invoked");
        final Pageable pageable = Pageable.ofSize(RECOMMENDATION_SIZE)
                .withPage(0);
        final List<ProductDetailResponse> contents = productRepository.findRecommendedProductsAsList(productId,
                                                                                                     pageable)
                .stream()
                .map(ProductDetailResponse::from)
                .toList();
        return new ProductDetailResponses(contents);
    }
}

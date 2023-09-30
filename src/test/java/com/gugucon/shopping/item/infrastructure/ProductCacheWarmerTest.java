package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.item.repository.ProductRepository;
import com.gugucon.shopping.item.repository.dto.ProductIdOrderIdPairDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCacheWarmerTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductCache productCache;

    @InjectMocks
    ProductCacheWarmer productCacheWarmer;

    /*
    1번 상품은 2, 3, 4번 상품과 각각 1, 2, 3회 함께 구매됨
    2번 상품은 1, 2, 4번 상품과 각각 1, 3, 2회 함께 구매됨
    3번 상품은 1, 2, 4번 상품과 각각 2, 3, 1회 함께 구매됨
    4번 상품은 1, 2, 3번 상품과 각각 3, 2, 1회 함께 구매됨
     */
    @Test
    void warmRecommendationCache() {
        // given
        when(productRepository.findAllId()).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(productRepository.findAllIdWithOrderId()).thenReturn(List.of(
                new ProductIdOrderIdPairDto(1L, 1L),
                new ProductIdOrderIdPairDto(4L, 1L),
                new ProductIdOrderIdPairDto(1L, 2L),
                new ProductIdOrderIdPairDto(4L, 2L),
                new ProductIdOrderIdPairDto(1L, 3L),
                new ProductIdOrderIdPairDto(3L, 3L),
                new ProductIdOrderIdPairDto(4L, 3L),
                new ProductIdOrderIdPairDto(2L, 4L),
                new ProductIdOrderIdPairDto(3L, 4L),
                new ProductIdOrderIdPairDto(2L, 5L),
                new ProductIdOrderIdPairDto(3L, 5L),
                new ProductIdOrderIdPairDto(1L, 6L),
                new ProductIdOrderIdPairDto(2L, 6L),
                new ProductIdOrderIdPairDto(3L, 6L),
                new ProductIdOrderIdPairDto(2L, 7L),
                new ProductIdOrderIdPairDto(4L, 7L),
                new ProductIdOrderIdPairDto(2L, 8L),
                new ProductIdOrderIdPairDto(4L, 8L)
        ));

        // when
        productCacheWarmer.warmRecommendationCache();

        // then
        verify(productCache).setRecommendationIds(1L, List.of(4L, 3L, 2L));
        verify(productCache).setRecommendationIds(2L, List.of(3L, 4L, 1L));
        verify(productCache).setRecommendationIds(3L, List.of(2L, 1L, 4L));
        verify(productCache).setRecommendationIds(4L, List.of(1L, 2L, 3L));
        verifyNoMoreInteractions(productCache);
    }
}

package com.gugucon.shopping.item.infrastructure;

import com.gugucon.shopping.item.repository.ProductRepository;
import com.gugucon.shopping.item.repository.dto.ProductIdOrderIdPairDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheWarmer {

    private final ProductRepository productRepository;
    private final ProductCache productCache;

    public void warmRecommendationCache() {
        log.info("recommendation cache warmer method invoked");
        final long start = System.currentTimeMillis();

        final List<Long> productIds = productRepository.findAllId();

        final List<ProductIdOrderIdPairDto> productIdOrderIdPairs = productRepository.findAllIdWithOrderId();

        final Map<Long, List<Long>> orderIdToProductIds = productIdOrderIdPairs.stream()
                .collect(Collectors.groupingBy(ProductIdOrderIdPairDto::getOrderId,
                                               Collectors.mapping(ProductIdOrderIdPairDto::getProductId,
                                                                  Collectors.toList())));

        final Map<Long, List<Long>> idToOrderedWithIds = new HashMap<>();
        for (Long productId : productIds) {
            idToOrderedWithIds.put(productId, new ArrayList<>());
        }

        for (List<Long> productIdsInOneOrder : orderIdToProductIds.values()) {
            final int size = productIdsInOneOrder.size();
            for (int i = 0; i < size; i++) {
                final Long pId1 = productIdsInOneOrder.get(i);
                for (int j = i + 1; j < size; j++) {
                    final Long pId2 = productIdsInOneOrder.get(j);
                    idToOrderedWithIds.get(pId1).add(pId2);
                    idToOrderedWithIds.get(pId2).add(pId1);
                }
            }
        }

        productIds.stream()
                .parallel()
                .forEach(productId -> singleWork(productId, idToOrderedWithIds));

        log.info("recommendation cache warmed, total elapsed time : {} ms", System.currentTimeMillis() - start);
    }

    private void singleWork(final Long productId, final Map<Long, List<Long>> idToOrderedWith) {
        final List<Long> recommendationIds = idToOrderedWith.get(productId)
                .stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Comparator.comparingLong(Map.Entry<Long, Long>::getValue).reversed())
                .map(Map.Entry::getKey)
                .limit(ProductCache.RECOMMENDATION_SIZE)
                .toList();

        productCache.setRecommendationIds(productId, recommendationIds);
    }
}

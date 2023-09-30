package com.gugucon.shopping.item.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheScheduler {

    private final ProductCache productCache;
    private final ProductCacheWarmer productCacheWarmer;

    @Scheduled(cron = "0 0 3 * * *")
    public void triggerCacheClearing() {
        log.info("cache eviction invoked");
        productCache.clearAll();
    }

    //@Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    public void triggerRecommendationCacheWarming() {
        productCacheWarmer.warmRecommendationCache();
    }
}

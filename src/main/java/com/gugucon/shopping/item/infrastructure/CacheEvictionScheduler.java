package com.gugucon.shopping.item.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheEvictionScheduler {

    @Scheduled(cron = "0 0 3 * * *")
    @CacheEvict(cacheNames = "recommendation", allEntries = true)
    public void clearCache() {
        log.info("cache eviction invoked");
    }
}

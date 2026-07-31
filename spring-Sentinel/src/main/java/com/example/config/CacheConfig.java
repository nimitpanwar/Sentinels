/**
 * CacheConfig
 *
 * PURPOSE: Caching for read-heavy, slow-changing data that can tolerate a
 *          few seconds of staleness. Deliberately NOT used for anything
 *          that must be exact "right now" - velocity counts and (future)
 *          daily-limit sums always run a live COUNT/SUM query against the
 *          DB, never a cache, since those are the hard threshold checks the
 *          fraud rules depend on being correct.
 *
 * CACHES:
 *   - historicalProfile: an account's behavioural baseline (mean/stddev
 *     amount, known payees/locations/categories) used by the anomaly-style
 *     rules (see HistoricalProfileService). Recomputing it means re-reading
 *     up to 90 days of transactions, so a short TTL absorbs bursts (e.g.
 *     many transactions in a few seconds for the same account, as in the
 *     velocity-rule test scenario) at the cost of a few seconds of
 *     staleness in the *baseline* only.
 *   - activeRules: the 'rules' table, read on every single transaction
 *     evaluation (see RiskEngine/RuleRepository). Rules only change when an
 *     operator edits one, so a 1-minute TTL is safe. NOTE: if/when a
 *     RuleController is added with update/delete endpoints, those should
 *     evict this cache (@CacheEvict("activeRules")) so edits take effect
 *     immediately instead of waiting out the TTL.
 */
package com.example.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache historicalProfile = new CaffeineCache("historicalProfile",
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(15, TimeUnit.SECONDS)
                        .build());

        CaffeineCache activeRules = new CaffeineCache("activeRules",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(historicalProfile, activeRules));
        return manager;
    }
}

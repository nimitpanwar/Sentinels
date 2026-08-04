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
 *     operator edits one, so a 1-minute TTL is safe. RuleController evicts
 *     this cache (@CacheEvict("activeRules")) on every create/update/delete
 *     so edits take effect immediately instead of waiting out the TTL.
 *   - alertSettings: the single alert_settings row (severity bands, alert
 *     threshold, merge cooldown - see AlertConfig/AlertSettingsController).
 *     Same reasoning as activeRules: rarely changes, short TTL, evicted on update.
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

        // Reference data looked up on every transaction creation - changes
        // rarely (new account/payee onboarding), so a longer TTL is safe.
        CaffeineCache accounts = new CaffeineCache("accounts",
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());
        CaffeineCache payees = new CaffeineCache("payees",
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());

        CaffeineCache alertSettings = new CaffeineCache("alertSettings",
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(historicalProfile, activeRules, accounts, payees, alertSettings));
        return manager;
    }
}

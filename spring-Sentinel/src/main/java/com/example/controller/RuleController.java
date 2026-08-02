package com.example.controller;

import com.example.dto.RuleRequest;
import com.example.entity.Rule;
import com.example.repository.RuleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * CRUD endpoints for monitoring rules - lets the frontend's Rules Management
 * screen create/edit/delete/toggle rules (weight, threshold, window, active)
 * without a redeploy.
 *
 * Every mutation evicts the "activeRules" cache (see CacheConfig and
 * RuleRepository.findByActiveTrue) so RiskEngine picks up the change on the
 * very next transaction instead of waiting out the cache TTL.
 */
@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleRepository ruleRepository;

    public RuleController(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping
    public ResponseEntity<List<Rule>> getAll() {
        return ResponseEntity.ok(ruleRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rule> getById(@PathVariable Integer id) {
        return ruleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @CacheEvict(value = "activeRules", allEntries = true)
    public ResponseEntity<Rule> create(@RequestBody RuleRequest request) {
        Rule rule = Rule.builder()
                .ruleName(request.getRuleName())
                .ruleType(request.getRuleType())
                .active(request.getActive() == null || request.getActive())
                .weight(request.getWeight() != null ? request.getWeight() : BigDecimal.ONE)
                .thresholdValue(request.getThresholdValue())
                .timeline(request.getTimeline() != null ? request.getTimeline() : 30)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleRepository.save(rule));
    }

    /** Partial update - only fields present in the request body are changed. */
    @PatchMapping("/{id}")
    @CacheEvict(value = "activeRules", allEntries = true)
    public ResponseEntity<Rule> update(@PathVariable Integer id, @RequestBody RuleRequest request) {
        return ruleRepository.findById(id).map(rule -> {
            if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
            if (request.getRuleType() != null) rule.setRuleType(request.getRuleType());
            if (request.getActive() != null) rule.setActive(request.getActive());
            if (request.getWeight() != null) rule.setWeight(request.getWeight());
            if (request.getThresholdValue() != null) rule.setThresholdValue(request.getThresholdValue());
            if (request.getTimeline() != null) rule.setTimeline(request.getTimeline());
            return ResponseEntity.ok(ruleRepository.save(rule));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "activeRules", allEntries = true)
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!ruleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ruleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

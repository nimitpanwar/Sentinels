package com.example.entity;

import com.example.enums.RuleType;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * JPA entity for the 'rules' table - fully DB-driven rule configuration.
 * Replaces the previous hardcoded RuleConfig: is_active/weight/threshold_value
 * are now editable directly in the DB without redeploying.
 *
 * Field re-use per rule type (documented since one column serves double duty):
 *  - AMOUNT_ANOMALY:   thresholdValue = z-score threshold (e.g. 3.00)
 *  - VELOCITY:         thresholdValue = transaction count threshold; timeline = lookback window in days
 *  - NEW_PAYEE:        thresholdValue = normalized risk score (0-1) contributed when triggered
 *  - TIME_ANOMALY:     thresholdValue = normalized risk score (0-1) contributed when triggered
 *  - LOCATION_CHANGE:  thresholdValue = normalized risk score (0-1) contributed when triggered
 *  - SPENDING_PATTERN: thresholdValue = normalized risk score (0-1) contributed when triggered
 *  - DEVICE_CHANGE:    unused (no Java rule implementation bound), seeded inactive
 */
@Entity
@Table(name = "rules")
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private RuleType ruleType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight;

    @Column(name = "threshold_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal thresholdValue;

    @Column(nullable = false)
    private int timeline = 30;

    public Rule() {
    }

    public Integer getRuleId() { return ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public int getTimeline() { return timeline; }
    public void setTimeline(int timeline) { this.timeline = timeline; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Rule rule = new Rule();

        public Builder ruleName(String ruleName) { rule.ruleName = ruleName; return this; }
        public Builder ruleType(RuleType ruleType) { rule.ruleType = ruleType; return this; }
        public Builder active(boolean active) { rule.active = active; return this; }
        public Builder weight(BigDecimal weight) { rule.weight = weight; return this; }
        public Builder thresholdValue(BigDecimal thresholdValue) { rule.thresholdValue = thresholdValue; return this; }
        public Builder timeline(int timeline) { rule.timeline = timeline; return this; }

        public Rule build() { return rule; }
    }
}

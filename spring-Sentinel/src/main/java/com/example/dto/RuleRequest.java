package com.example.dto;

import com.example.enums.RuleType;

import java.math.BigDecimal;

/**
 * Request body for creating/editing a monitoring rule via the API (used by
 * RuleController). Fields are boxed types (not primitives) so PATCH updates
 * can tell "not provided, leave unchanged" apart from an explicit false/zero.
 */
public class RuleRequest {

    private String ruleName;
    private RuleType ruleType;
    private Boolean active;
    private BigDecimal weight;
    private BigDecimal thresholdValue;
    private Integer timeline; // window - unit depends on rule type, see entity/Rule.java (minutes for VELOCITY)

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public RuleType getRuleType() { return ruleType; }
    public void setRuleType(RuleType ruleType) { this.ruleType = ruleType; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    public Integer getTimeline() { return timeline; }
    public void setTimeline(Integer timeline) { this.timeline = timeline; }
}

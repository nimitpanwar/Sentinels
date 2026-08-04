package com.example.dto;

import java.util.Map;

/** Aggregate stats for the Alerts/Cases dashboard - see CaseController#stats. */
public class CaseStatsResponse {

    private Map<String, Long> countByStatus;
    private Double avgMinutesToAcknowledge;
    private Double avgMinutesToClose;
    private long totalCases;

    public Map<String, Long> getCountByStatus() { return countByStatus; }
    public void setCountByStatus(Map<String, Long> countByStatus) { this.countByStatus = countByStatus; }
    public Double getAvgMinutesToAcknowledge() { return avgMinutesToAcknowledge; }
    public void setAvgMinutesToAcknowledge(Double avgMinutesToAcknowledge) { this.avgMinutesToAcknowledge = avgMinutesToAcknowledge; }
    public Double getAvgMinutesToClose() { return avgMinutesToClose; }
    public void setAvgMinutesToClose(Double avgMinutesToClose) { this.avgMinutesToClose = avgMinutesToClose; }
    public long getTotalCases() { return totalCases; }
    public void setTotalCases(long totalCases) { this.totalCases = totalCases; }
}

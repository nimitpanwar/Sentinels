package com.example.dto;

import java.time.LocalDateTime;

public class AlertHistoryEventResponse {
    private String eventType;
    private String title;
    private LocalDateTime at;
    private String details;

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getAt() { return at; }
    public void setAt(LocalDateTime at) { this.at = at; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}

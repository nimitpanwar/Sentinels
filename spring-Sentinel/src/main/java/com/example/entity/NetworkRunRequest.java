package com.example.entity;

import com.example.enums.NetworkRunRequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * JPA entity for the 'network_run_requests' table - the DB-mediated "Run
 * Analysis Now" trigger queue. Spring inserts a PENDING row when the
 * operator clicks the button in the UI; the Python job polls this table on
 * its own schedule and picks up any PENDING row early instead of waiting
 * for its normal interval. Deliberately NOT a direct HTTP call from Spring
 * into the Python process - keeps the two components fully decoupled,
 * consistent with "both sides only talk to the database".
 */
@Entity
@Table(name = "network_run_requests")
public class NetworkRunRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "lookback_days", nullable = false)
    private Integer lookbackDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NetworkRunRequestStatus status = NetworkRunRequestStatus.PENDING;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    public NetworkRunRequest() {
    }

    @PrePersist
    public void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public Integer getLookbackDays() { return lookbackDays; }
    public void setLookbackDays(Integer lookbackDays) { this.lookbackDays = lookbackDays; }
    public NetworkRunRequestStatus getStatus() { return status; }
    public void setStatus(NetworkRunRequestStatus status) { this.status = status; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }
}

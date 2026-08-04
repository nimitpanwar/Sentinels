package com.example.repository;

import com.example.entity.NetworkRunRequest;
import com.example.enums.NetworkRunRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NetworkRunRequestRepository extends JpaRepository<NetworkRunRequest, Long> {

    Optional<NetworkRunRequest> findFirstByStatusOrderByRequestedAtAsc(NetworkRunRequestStatus status);
}

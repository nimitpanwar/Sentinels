package com.example.repository;

import com.example.entity.NetworkRunRequest;
import com.example.enums.NetworkRunRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkRunRequestRepository extends JpaRepository<NetworkRunRequest, Long> {

    Optional<NetworkRunRequest> findFirstByStatusOrderByRequestedAtAsc(NetworkRunRequestStatus status);
}

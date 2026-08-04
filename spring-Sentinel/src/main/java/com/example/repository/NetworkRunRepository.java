package com.example.repository;

import com.example.entity.NetworkRun;
import com.example.enums.NetworkRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkRunRepository extends JpaRepository<NetworkRun, Integer> {

    /** Most recent successfully-completed run - this is the run whose scores the API/UI shows "live". */
    Optional<NetworkRun> findFirstByStatusOrderByCompletedAtDesc(NetworkRunStatus status);

    Page<NetworkRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}

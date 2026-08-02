package com.example.repository;

import com.example.entity.AlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Integer> {
}

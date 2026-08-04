package com.example.repository;

import com.example.entity.AlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Integer> {
}

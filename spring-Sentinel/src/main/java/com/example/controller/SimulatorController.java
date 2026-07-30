/**
 * SimulatorController
 * 
 * PURPOSE: REST API endpoints for controlling and triggering the transaction
 *          simulator. Used for testing, demoing, and triggering specific
 *          rule-tripping scenarios on demand.
 * 
 * ENDPOINTS:
 *   - POST /api/simulator/start: Turn on automatic transaction generation
 *     Response: {"running": true}
 *   
 *   - POST /api/simulator/stop: Turn off automatic transaction generation
 *     Response: {"running": false}
 *   
 *   - GET /api/simulator/status: Check if simulator is currently running
 *     Response: {"running": true/false}
 *   
 *   - POST /api/simulator/trigger/{scenario}: Manually fire a specific scenario
 *     Scenarios: velocity, high-value, new-payee
 *     Example: POST /api/simulator/trigger/velocity
 *     Response: Scenario name and details
 * 
 * USE CASES:
 *   - Start/stop the simulator to control data generation
 *   - Trigger specific scenarios to test how alerts are created
 *   - Pause to inspect database state without new data interfering
 *   - On-demand scenario generation for demo purposes
 */
package com.example.controller;

import com.example.service.TransactionSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final TransactionSimulator simulator;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        simulator.start();
        return ResponseEntity.ok(Map.of("running", true, "message", "Simulator started"));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        simulator.stop();
        return ResponseEntity.ok(Map.of("running", false, "message", "Simulator stopped"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("running", simulator.isRunning()));
    }

    @PostMapping("/trigger/{scenario}")
    public ResponseEntity<Map<String, Object>> trigger(@PathVariable String scenario) {
        return switch (scenario.toLowerCase()) {
            case "velocity" -> {
                simulator.triggerVelocityScenario();
                yield ResponseEntity.ok(Map.of("scenario", "velocity", "message", "Velocity scenario triggered (6 rapid transactions)"));
            }
            case "high-value" -> {
                simulator.triggerHighValueScenario();
                yield ResponseEntity.ok(Map.of("scenario", "high-value", "message", "High-value scenario triggered"));
            }
            case "new-payee" -> {
                simulator.triggerNewPayeeScenario();
                yield ResponseEntity.ok(Map.of("scenario", "new-payee", "message", "New-payee scenario triggered"));
            }
            default -> ResponseEntity.badRequest()
                    .body(Map.of("error", "Unknown scenario: " + scenario,
                                 "available", "velocity, high-value, new-payee"));
        };
    }
}

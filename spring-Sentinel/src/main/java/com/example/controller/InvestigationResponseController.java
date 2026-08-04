package com.example.controller;

import com.example.dto.InvestigationResponseFormContextResponse;
import com.example.dto.InvestigationResponseFormRequest;
import com.example.dto.InvestigationResponseReceiptResponse;
import com.example.service.InvestigationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/investigation/respond", "/api/investigation/respond"})
public class InvestigationResponseController {

    private final InvestigationService investigationService;

    public InvestigationResponseController(InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<InvestigationResponseFormContextResponse> getContext(@PathVariable String token) {
        return ResponseEntity.ok(investigationService.getResponseFormContext(token));
    }

    @PostMapping("/{token}")
    public ResponseEntity<InvestigationResponseReceiptResponse> submit(@PathVariable String token,
                                                                        @RequestBody InvestigationResponseFormRequest request,
                                                                        HttpServletRequest servletRequest) {
        String sourceIp = servletRequest.getRemoteAddr();
        String userAgent = servletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(investigationService.submitCustomerResponse(token, request, sourceIp, userAgent));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "Invalid request" : ex.getMessage();
        if (message.toLowerCase().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
        }
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(IllegalStateException ex) {
        String message = ex.getMessage() == null ? "Invalid state" : ex.getMessage();
        if (message.toLowerCase().contains("expired") || message.toLowerCase().contains("already been used")) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", message));
        }
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
}
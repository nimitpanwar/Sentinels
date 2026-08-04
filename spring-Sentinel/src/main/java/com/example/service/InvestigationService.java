package com.example.service;

import com.example.dto.InvestigationMessageResponse;
import com.example.dto.InvestigationSendRequest;
import com.example.dto.InvestigationSendResponse;
import com.example.entity.Alert;
import com.example.entity.Case;
import com.example.entity.Customer;
import com.example.entity.InvestigationMessage;
import com.example.enums.CaseStatus;
import com.example.enums.InvestigationMessageStatus;
import com.example.repository.AlertRepository;
import com.example.repository.InvestigationMessageRepository;
import com.example.riskengine.alert.AlertManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class InvestigationService {

    private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);
    private static final int MAX_SUBJECT_LENGTH = 255;
    private static final int MAX_BODY_SNAPSHOT_LENGTH = 240;
    private static final String DEMO_FROM_EMAIL = "aaryaprasadpai.cy22@rvce.edu.in";
    private static final String DEMO_TO_EMAIL = "aaryaipl8@gmail.com";

    private final AlertRepository alertRepository;
    private final InvestigationMessageRepository messageRepository;
    private final AlertManager alertManager;
    private final JavaMailSender mailSender;

    @Value("${app.investigation.email.override-enabled:true}")
    private boolean overrideEnabled;

    @Value("${app.investigation.email.override-recipient:}")
    private String overrideRecipient;

    @Value("${app.investigation.response.token-ttl-hours:72}")
    private int tokenTtlHours;

    @Value("${app.investigation.email.from:}")
    private String fromEmail;

    @Value("${app.investigation.email.mode:stub}")
    private String emailMode;

    public InvestigationService(AlertRepository alertRepository,
                                InvestigationMessageRepository messageRepository,
                                AlertManager alertManager,
                                JavaMailSender mailSender) {
        this.alertRepository = alertRepository;
        this.messageRepository = messageRepository;
        this.alertManager = alertManager;
        this.mailSender = mailSender;
    }

    @Transactional
    public Alert markInvestigating(Integer alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        Case aCase = alert.getCase();
        if (aCase == null) {
            throw new IllegalStateException("Alert " + alertId + " has no case to investigate");
        }

        moveCaseToInvestigating(aCase);
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found after investigate transition: " + alertId));
    }

    @Transactional(readOnly = true)
    public List<InvestigationMessageResponse> getThread(Integer alertId) {
        assertAlertExists(alertId);
        return messageRepository.findByAlertAlertIdOrderByCreatedAtDesc(alertId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InvestigationSendResponse send(Integer alertId, InvestigationSendRequest request) {
        if (request == null || isBlank(request.getSubject()) || isBlank(request.getBody())) {
            throw new IllegalArgumentException("Both 'subject' and 'body' are required");
        }

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        Case aCase = alert.getCase();
        if (aCase == null) {
            throw new IllegalStateException("Alert " + alertId + " has no case to investigate");
        }

        moveCaseIntoInvestigatingIfNeeded(aCase);

        Alert refreshedAlert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found after transition: " + alertId));

        String intendedRecipient = extractCustomerEmail(refreshedAlert);
        String deliveredRecipient = DEMO_TO_EMAIL;

        InvestigationMessage msg = new InvestigationMessage();
        msg.setAlert(refreshedAlert);
        msg.setCase(refreshedAlert.getCase());
        msg.setIntendedRecipientEmail(intendedRecipient);
        msg.setDeliveredRecipientEmail(deliveredRecipient);
        msg.setSubject(clamp(request.getSubject().trim(), MAX_SUBJECT_LENGTH));
        msg.setBodySnapshot(clamp(request.getBody().trim(), MAX_BODY_SNAPSHOT_LENGTH));
        msg.setResponseToken(generateToken());
        msg.setResponseTokenExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(tokenTtlHours));

        if ("smtp".equalsIgnoreCase(emailMode)) {
            sendViaSmtp(deliveredRecipient, request.getSubject().trim(), request.getBody().trim(), msg.getResponseToken());
            msg.setDeliveryStatus(InvestigationMessageStatus.SENT);
            msg.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
            log.info("Investigation email delivered via SMTP for alert {} to {} (demo hardcoded recipient)",
                    alertId, deliveredRecipient);
        } else {
            // Stub mode records dispatch intent without contacting external providers.
            msg.setDeliveryStatus(InvestigationMessageStatus.SENT);
            msg.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
            log.info("Investigation outreach recorded in STUB mode for alert {} to {} (demo hardcoded recipient)",
                    alertId, deliveredRecipient);
        }

        InvestigationMessage saved = messageRepository.save(msg);
        return new InvestigationSendResponse(refreshedAlert, toResponse(saved));
    }

    private void sendViaSmtp(String recipient, String subject, String body, String token) {
        String responseLink = "http://localhost:5173/investigation/respond/" + token;
        String renderedBody = body.replace("{{response_link}}", responseLink);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(DEMO_FROM_EMAIL);
            message.setTo(DEMO_TO_EMAIL);
            message.setSubject(clamp(subject, MAX_SUBJECT_LENGTH));
            message.setText(renderedBody);
            mailSender.send(message);
        } catch (MailException ex) {
            throw new IllegalStateException("SMTP delivery failed: " + ex.getMessage());
        }
    }

    private void moveCaseIntoInvestigatingIfNeeded(Case aCase) {
        CaseStatus status = aCase.getStatus();
        if (status == CaseStatus.OPEN) {
            // Keep send resilient across environments where legacy DB enum values
            // may reject INVESTIGATING. We only need analyst ownership at send time.
            alertManager.acknowledge(aCase.getCaseId());
            return;
        }
        if (status == CaseStatus.ACKNOWLEDGED) {
            return;
        }
        if (status == CaseStatus.INVESTIGATING || status == CaseStatus.IN_REVIEW || status == CaseStatus.ESCALATED) {
            return;
        }
        throw new IllegalStateException("Cannot investigate case in status: " + status);
    }

    private void moveCaseToInvestigating(Case aCase) {
        CaseStatus status = aCase.getStatus();
        if (status == CaseStatus.OPEN) {
            alertManager.acknowledge(aCase.getCaseId());
            alertManager.investigate(aCase.getCaseId());
            return;
        }
        if (status == CaseStatus.ACKNOWLEDGED) {
            alertManager.investigate(aCase.getCaseId());
            return;
        }
        if (status == CaseStatus.INVESTIGATING || status == CaseStatus.IN_REVIEW || status == CaseStatus.ESCALATED) {
            return;
        }
        throw new IllegalStateException("Cannot investigate case in status: " + status);
    }

    private String clamp(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        log.warn("Truncating investigation message field from {} to {} characters", value.length(), maxLength);
        return value.substring(0, maxLength);
    }

    private InvestigationMessageResponse toResponse(InvestigationMessage msg) {
        InvestigationMessageResponse res = new InvestigationMessageResponse();
        res.setMessageId(msg.getMessageId());
        res.setAlertId(msg.getAlert().getAlertId());
        res.setCaseId(msg.getCase().getCaseId());
        res.setIntendedRecipientEmail(msg.getIntendedRecipientEmail());
        res.setDeliveredRecipientEmail(msg.getDeliveredRecipientEmail());
        res.setSubject(msg.getSubject());
        res.setBodySnapshot(msg.getBodySnapshot());
        res.setDeliveryStatus(msg.getDeliveryStatus());
        res.setDeliveryError(msg.getDeliveryError());
        res.setCreatedAt(msg.getCreatedAt());
        res.setSentAt(msg.getSentAt());
        res.setResponseTokenExpiresAt(msg.getResponseTokenExpiresAt());
        return res;
    }

    private String extractCustomerEmail(Alert alert) {
        Customer customer = null;
        if (alert.getTransaction() != null && alert.getTransaction().getAccount() != null) {
            customer = alert.getTransaction().getAccount().getCustomer();
        }
        if (customer == null || isBlank(customer.getEmail())) {
            return null;
        }
        return customer.getEmail().trim();
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void assertAlertExists(Integer alertId) {
        if (!alertRepository.existsById(alertId)) {
            throw new IllegalArgumentException("Alert not found: " + alertId);
        }
    }
}

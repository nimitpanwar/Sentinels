package com.example.service;

import com.example.dto.InvestigationAnalystActionRequest;
import com.example.dto.InvestigationMessageResponse;
import com.example.dto.InvestigationResponseFormContextResponse;
import com.example.dto.InvestigationResponseFormRequest;
import com.example.dto.InvestigationResponseReceiptResponse;
import com.example.dto.InvestigationSendRequest;
import com.example.dto.InvestigationSendResponse;
import com.example.entity.Alert;
import com.example.entity.Case;
import com.example.entity.Customer;
import com.example.entity.InvestigationMessage;
import com.example.entity.InvestigationResponse;
import com.example.enums.CaseStatus;
import com.example.enums.InvestigationMessageStatus;
import com.example.enums.InvestigationResponseStatus;
import com.example.repository.AlertRepository;
import com.example.repository.InvestigationMessageRepository;
import com.example.repository.InvestigationResponseRepository;
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
import java.util.Locale;
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
    private final InvestigationResponseRepository responseRepository;
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

    @Value("${app.investigation.response.public-base-url:http://localhost:5173}")
    private String responsePublicBaseUrl;

    public InvestigationService(AlertRepository alertRepository,
                                InvestigationMessageRepository messageRepository,
                                InvestigationResponseRepository responseRepository,
                                AlertManager alertManager,
                                JavaMailSender mailSender) {
        this.alertRepository = alertRepository;
        this.messageRepository = messageRepository;
        this.responseRepository = responseRepository;
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
                .map(msg -> toResponse(msg, responseRepository.findByMessageMessageId(msg.getMessageId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public InvestigationResponseFormContextResponse getResponseFormContext(String token) {
        InvestigationMessage msg = findMessageByToken(token);
        updateExpiryStateIfNeeded(msg);

        if (msg.getResponseStatus() == InvestigationResponseStatus.EXPIRED) {
            throw new IllegalStateException("This response link has expired.");
        }

        InvestigationResponseFormContextResponse res = new InvestigationResponseFormContextResponse();
        res.setMessageId(msg.getMessageId());
        res.setAlertId(msg.getAlert().getAlertId());
        res.setCaseId(msg.getCase().getCaseId());
        res.setRecipientEmailMasked(maskEmail(msg.getDeliveredRecipientEmail()));
        res.setSubject(msg.getSubject());
        res.setSentAt(msg.getSentAt());
        res.setResponseTokenExpiresAt(msg.getResponseTokenExpiresAt());
        res.setAlreadySubmitted(msg.getResponseStatus() == InvestigationResponseStatus.RESPONDED);
        return res;
    }

    @Transactional
    public InvestigationResponseReceiptResponse submitCustomerResponse(String token,
                                                                      InvestigationResponseFormRequest request,
                                                                      String sourceIp,
                                                                      String userAgent) {
        validateResponseRequest(request);
        InvestigationMessage msg = findMessageByToken(token);
        updateExpiryStateIfNeeded(msg);

        if (msg.getResponseStatus() == InvestigationResponseStatus.EXPIRED) {
            throw new IllegalStateException("This response link has expired.");
        }
        if (msg.getResponseStatus() == InvestigationResponseStatus.RESPONDED || msg.getTokenConsumedAt() != null) {
            throw new IllegalStateException("This response link has already been used.");
        }

        InvestigationResponse response = new InvestigationResponse();
        response.setMessage(msg);
        response.setRecognizedTransaction(request.getRecognizedTransaction());
        response.setAuthorizedTransaction(request.getAuthorizedTransaction());
        response.setExplanation(request.getExplanation().trim());
        response.setRespondentName(request.getRespondentName().trim());
        response.setRespondentEmail(request.getRespondentEmail().trim());
        response.setSourceIp(sourceIp);
        response.setUserAgent(userAgent);
        InvestigationResponse saved = responseRepository.save(response);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        msg.setResponseStatus(InvestigationResponseStatus.RESPONDED);
        msg.setRespondedAt(now);
        msg.setTokenConsumedAt(now);
        messageRepository.save(msg);

        InvestigationResponseReceiptResponse receipt = new InvestigationResponseReceiptResponse();
        receipt.setResponseId(saved.getResponseId());
        receipt.setMessageId(msg.getMessageId());
        receipt.setSubmittedAt(saved.getSubmittedAt());
        receipt.setMessage("Thank you. Your response has been received.");
        return receipt;
    }

    @Transactional
    public InvestigationSendResponse applyAnalystAction(Integer alertId, InvestigationAnalystActionRequest request) {
        if (request == null || isBlank(request.getAction())) {
            throw new IllegalArgumentException("Action is required");
        }

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        Case aCase = alert.getCase();
        if (aCase == null) {
            throw new IllegalStateException("Alert " + alertId + " has no case");
        }

        String action = request.getAction().trim().toUpperCase(Locale.ROOT);
        return switch (action) {
            case "DISMISS" -> {
                alertManager.dismiss(aCase.getCaseId(), request.getAnalystNotes(), null)
                        .orElseThrow(() -> new IllegalArgumentException("Case not found: " + aCase.getCaseId()));
                Alert updated = alertRepository.findById(alertId)
                        .orElseThrow(() -> new IllegalArgumentException("Alert not found after dismiss: " + alertId));
                yield new InvestigationSendResponse(updated, latestThreadMessage(alertId));
            }
            case "FLAG" -> {
                alertManager.escalate(aCase.getCaseId(), request.getAnalystNotes(), null)
                        .orElseThrow(() -> new IllegalArgumentException("Case not found: " + aCase.getCaseId()));
                Alert updated = alertRepository.findById(alertId)
                        .orElseThrow(() -> new IllegalArgumentException("Alert not found after escalate: " + alertId));
                yield new InvestigationSendResponse(updated, latestThreadMessage(alertId));
            }
            case "REQUEST_MORE_INFO" -> {
                if (isBlank(request.getSubject()) || isBlank(request.getBody())) {
                    throw new IllegalArgumentException("'subject' and 'body' are required for REQUEST_MORE_INFO");
                }
                yield send(alertId, new InvestigationSendRequestBuilder(request.getSubject(), request.getBody()).build());
            }
            default -> throw new IllegalArgumentException("Unsupported action: " + request.getAction());
        };
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
        msg.setResponseStatus(InvestigationResponseStatus.AWAITING_RESPONSE);

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
        return new InvestigationSendResponse(refreshedAlert, toResponse(saved, null));
    }

    private void sendViaSmtp(String recipient, String subject, String body, String token) {
        String responseLink = buildResponseLink(token);
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

    private String buildResponseLink(String token) {
        String base = responsePublicBaseUrl == null ? "" : responsePublicBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            base = "http://localhost:5173";
        }
        return base + "/investigation/respond/" + token;
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

    private InvestigationMessageResponse toResponse(InvestigationMessage msg, InvestigationResponse response) {
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
        res.setResponseStatus(msg.getResponseStatus());
        res.setRespondedAt(msg.getRespondedAt());
        if (response != null) {
            res.setRecognizedTransaction(response.getRecognizedTransaction());
            res.setAuthorizedTransaction(response.getAuthorizedTransaction());
            res.setResponseExplanation(response.getExplanation());
            res.setRespondentName(response.getRespondentName());
            res.setRespondentEmail(response.getRespondentEmail());
        }
        return res;
    }

    private InvestigationMessageResponse latestThreadMessage(Integer alertId) {
        return messageRepository.findTopByAlertAlertIdOrderByCreatedAtDesc(alertId)
                .map(msg -> toResponse(msg, responseRepository.findByMessageMessageId(msg.getMessageId()).orElse(null)))
                .orElse(null);
    }

    private InvestigationMessage findMessageByToken(String token) {
        if (isBlank(token)) {
            throw new IllegalArgumentException("Response token is required");
        }
        return messageRepository.findByResponseToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Response token not found"));
    }

    private void validateResponseRequest(InvestigationResponseFormRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Response payload is required");
        }
        if (request.getRecognizedTransaction() == null) {
            throw new IllegalArgumentException("recognizedTransaction is required");
        }
        if (request.getAuthorizedTransaction() == null) {
            throw new IllegalArgumentException("authorizedTransaction is required");
        }
        if (isBlank(request.getExplanation())) {
            throw new IllegalArgumentException("explanation is required");
        }
        if (isBlank(request.getRespondentName())) {
            throw new IllegalArgumentException("respondentName is required");
        }
        if (isBlank(request.getRespondentEmail())) {
            throw new IllegalArgumentException("respondentEmail is required");
        }
    }

    private void updateExpiryStateIfNeeded(InvestigationMessage msg) {
        if (msg.getResponseStatus() == InvestigationResponseStatus.RESPONDED) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (msg.getResponseTokenExpiresAt() != null && msg.getResponseTokenExpiresAt().isBefore(now)) {
            msg.setResponseStatus(InvestigationResponseStatus.EXPIRED);
            messageRepository.save(msg);
        }
    }

    private String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return "masked";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return "**@" + parts[1];
        }
        return local.substring(0, 2) + "***@" + parts[1];
    }

    private static final class InvestigationSendRequestBuilder {
        private final String subject;
        private final String body;

        private InvestigationSendRequestBuilder(String subject, String body) {
            this.subject = subject;
            this.body = body;
        }

        private InvestigationSendRequest build() {
            InvestigationSendRequest request = new InvestigationSendRequest();
            request.setSubject(subject);
            request.setBody(body);
            return request;
        }
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

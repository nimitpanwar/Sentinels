/**
 * ChatbotController
 *
 * PURPOSE: REST endpoint for the rule/alert Q&A chatbot. Answers are grounded
 *          in the 'rules' table and local risk-engine documentation only -
 *          see ChatbotService for the no-web-search system prompt.
 */
package com.example.controller;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;
import com.example.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse("Please provide a question."));
        }
        return ResponseEntity.ok(new ChatResponse(chatbotService.askQuestion(request.getQuestion())));
    }
}

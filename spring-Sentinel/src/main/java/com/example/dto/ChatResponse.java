package com.example.dto;

/** Response body for POST /api/chatbot/ask. */
public class ChatResponse {

    private String answer;

    public ChatResponse() {
    }

    public ChatResponse(String answer) {
        this.answer = answer;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}

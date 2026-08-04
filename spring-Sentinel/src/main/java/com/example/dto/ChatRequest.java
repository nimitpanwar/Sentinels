package com.example.dto;

/** Request body for POST /api/chatbot/ask. */
public class ChatRequest {

    private String question;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}

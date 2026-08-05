/**
 * chatbotApi.js
 *
<<<<<<< HEAD
 * Talks to the Groq-backed rule assistant: POST /api/chatbot/ask
 * Body:  { question: string }
 * Reply: { answer: string }
 */
const BASE = '/api/chatbot';

export async function askQuestion(question) {
  const res = await fetch(`${BASE}/ask`, {
=======
 * Thin wrapper around the backend's Groq-backed rule/alert Q&A chatbot
 * (see controller/ChatbotController.java + service/ChatbotService.java).
 * Answers are grounded only in the local rules table and risk-engine docs.
 */

/** Ask the assistant a question and get back its grounded answer. */
export async function askAgent(question) {
  const res = await fetch('/api/chatbot/ask', {
>>>>>>> master
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });
<<<<<<< HEAD

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Assistant request failed (${res.status})`);
  }

  return res.json();
=======
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.answer || data.message || `Failed to reach agent (HTTP ${res.status})`);
  }
  return data.answer;
>>>>>>> master
}

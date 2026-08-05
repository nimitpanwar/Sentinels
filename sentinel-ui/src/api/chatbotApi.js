/**
 * chatbotApi.js
 *
 * Talks to the Groq-backed rule assistant: POST /api/chatbot/ask
 * Body:  { question: string }
 * Reply: { answer: string }
 */
const BASE = '/api/chatbot';

/** Ask the assistant a question and get back its grounded answer. */
export async function askAgent(question) {
  const res = await fetch(`${BASE}/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.answer || data.message || `Failed to reach agent (HTTP ${res.status})`);
  }
  return data.answer;
}


/**
 * chatbotApi.js
 *
 * Talks to the Groq-backed rule assistant: POST /api/chatbot/ask
 * Body:  { question: string }
 * Reply: { answer: string }
 */
const BASE = '/api/chatbot';

export async function askQuestion(question) {
  const res = await fetch(`${BASE}/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Assistant request failed (${res.status})`);
  }

  return res.json();
}

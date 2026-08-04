const BASE = '/api/chatbot';

export async function askChatbot(question) {
  const res = await fetch(`${BASE}/ask`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ question }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Chatbot request failed: ${res.status}`);
  }
  return res.json();
}

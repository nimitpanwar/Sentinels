import { useState, useRef, useEffect } from 'react';
import { askChatbot } from '../../api/chatbotApi';
import '../transactions/transactions.css';
import './chatbot.css';

export default function ChatbotPage() {
  const [messages, setMessages] = useState([
    { role: 'assistant', text: 'Ask me about rules, alerts, or risk scoring logic.' },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function handleSend(e) {
    e.preventDefault();
    const question = input.trim();
    if (!question || sending) return;

    setMessages(prev => [...prev, { role: 'user', text: question }]);
    setInput('');
    setSending(true);
    setError(null);

    try {
      const { answer } = await askChatbot(question);
      setMessages(prev => [...prev, { role: 'assistant', text: answer }]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="page-chatbot">
      <div className="page-header">
        <h1 className="page-title">Assistant</h1>
        <span className="row-count">Rule &amp; alert Q&amp;A</span>
      </div>

      <div className="chatbot-panel">
        <div className="chatbot-messages">
          {messages.map((m, i) => (
            <div key={i} className={`chatbot-msg chatbot-msg--${m.role}`}>
              <span className="chatbot-msg__role">{m.role === 'user' ? 'You' : 'Sentinel'}</span>
              <p className="chatbot-msg__text">{m.text}</p>
            </div>
          ))}
          {sending && (
            <div className="chatbot-msg chatbot-msg--assistant chatbot-msg--pending">
              <span className="chatbot-msg__role">Sentinel</span>
              <p className="chatbot-msg__text">Thinking…</p>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        {error && <div className="error-banner">{error}</div>}

        <form className="chatbot-input-row" onSubmit={handleSend}>
          <input
            className="chatbot-input"
            type="text"
            placeholder="Ask about a rule, alert, or risk score…"
            value={input}
            onChange={e => setInput(e.target.value)}
            disabled={sending}
          />
          <button className="chatbot-send-btn" type="submit" disabled={sending || !input.trim()}>
            Send
          </button>
        </form>
      </div>
    </div>
  );
}

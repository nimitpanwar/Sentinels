import { useState, useRef, useEffect } from 'react';
import { askQuestion } from '../../api/chatbotApi';
import './assistant.css';

const SUGGESTED_QUESTIONS = [
  'What rules are currently active?',
  'How does the velocity rule work?',
  'What triggers a high-value transaction alert?',
  'How is the overall risk score calculated?',
];

export default function AssistantPage() {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: 'Ask me anything about the fraud-detection rules or how alerts are generated.' },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const threadRef = useRef(null);

  useEffect(() => {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, sending]);

  async function handleSend(question) {
    const q = (question ?? input).trim();
    if (!q || sending) return;

    setError(null);
    setMessages(prev => [...prev, { role: 'user', content: q }]);
    setInput('');
    setSending(true);

    try {
      const { answer } = await askQuestion(q);
      setMessages(prev => [...prev, { role: 'assistant', content: answer }]);
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  }

  function handleSubmit(e) {
    e.preventDefault();
    handleSend();
  }

  return (
    <div className="page-assistant">
      <div className="page-header">
        <h1 className="page-title">Assistant</h1>
        <span className="assistant-subtitle">Rule &amp; alert Q&amp;A — grounded in the live rule set</span>
      </div>

      <div className="assistant-panel">
        <div className="assistant-thread" ref={threadRef}>
          {messages.map((m, i) => (
            <div key={i} className={`assistant-message assistant-message--${m.role}`}>
              <div className="assistant-message__role">{m.role === 'user' ? 'You' : 'Sentinel'}</div>
              <div className="assistant-message__body">{m.content}</div>
            </div>
          ))}
          {sending && (
            <div className="assistant-message assistant-message--assistant">
              <div className="assistant-message__role">Sentinel</div>
              <div className="assistant-message__body assistant-message__body--pending">Thinking…</div>
            </div>
          )}
        </div>

        {error && <p className="assistant-error">{error}</p>}

        <div className="assistant-suggestions">
          {SUGGESTED_QUESTIONS.map((q) => (
            <button
              key={q}
              type="button"
              className="assistant-suggestion"
              onClick={() => handleSend(q)}
              disabled={sending}
            >
              {q}
            </button>
          ))}
        </div>

        <form className="assistant-input-bar" onSubmit={handleSubmit}>
          <input
            className="assistant-input"
            type="text"
            placeholder="Ask about a rule, e.g. “Why would this trigger the velocity rule?”"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={sending}
          />
          <button type="submit" className="assistant-send-btn" disabled={sending || !input.trim()}>
            Send
          </button>
        </form>
      </div>
    </div>
  );
}

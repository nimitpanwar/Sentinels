import { useEffect, useRef, useState } from 'react';
import { askAgent } from '../../api/chatbotApi';
import './agent.css';

let nextId = 1;

export default function AgentPage() {
  const [messages, setMessages] = useState([
    {
      id: nextId++,
      role: 'assistant',
      text: 'Ask me about rules, alerts, or how the risk engine works. Answers are grounded only in this workspace\u2019s rules and documentation \u2014 no web search.',
    },
  ]);
  const [question, setQuestion] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState(null);
  const logRef = useRef(null);

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, [messages, sending]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const trimmed = question.trim();
    if (!trimmed || sending) return;

    setMessages(prev => [...prev, { id: nextId++, role: 'user', text: trimmed }]);
    setQuestion('');
    setError(null);
    setSending(true);

    try {
      const answer = await askAgent(trimmed);
      setMessages(prev => [...prev, { id: nextId++, role: 'assistant', text: answer }]);
    } catch (err) {
      setError(err.message);
      setMessages(prev => [...prev, { id: nextId++, role: 'error', text: err.message }]);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="page-agent">
      <div className="page-header">
        <div>
          <div className="overview-eyebrow">Groq-Powered Assistant</div>
          <h1 className="page-title">Agent</h1>
        </div>
        <span className="row-count">Grounded in local rules &amp; risk-engine docs &mdash; no web search</span>
      </div>

      <section className="agent-panel">
        <div className="agent-log" ref={logRef}>
          {messages.map(m => (
            <div key={m.id} className={`agent-msg agent-msg--${m.role}`}>
              <div className="agent-msg__role">
                {m.role === 'user' ? 'You' : m.role === 'error' ? 'Error' : 'Agent'}
              </div>
              <div className="agent-msg__text">{m.text}</div>
            </div>
          ))}
          {sending && (
            <div className="agent-msg agent-msg--assistant agent-msg--pending">
              <div className="agent-msg__role">Agent</div>
              <div className="agent-msg__text">Thinking…</div>
            </div>
          )}
        </div>

        <form className="agent-input-row" onSubmit={handleSubmit}>
          <input
            type="text"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="e.g. What triggers a NEW_PAYEE alert?"
            autoComplete="off"
            disabled={sending}
          />
          <button type="submit" className="agent-send-btn" disabled={sending || !question.trim()}>
            Send
          </button>
        </form>
        {error && <div className="error-banner">Error: {error}</div>}
      </section>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  fetchInvestigationResponseContext,
  submitInvestigationResponse,
} from '../../api/alertsApi';
import '../alerts/alerts.css';

export default function CustomerResponsePage() {
  const { token } = useParams();
  const [context, setContext] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(null);
  const [error, setError] = useState(null);

  const [recognizedTransaction, setRecognizedTransaction] = useState(null);
  const [authorizedTransaction, setAuthorizedTransaction] = useState(null);
  const [explanation, setExplanation] = useState('');
  const [respondentName, setRespondentName] = useState('');
  const [respondentEmail, setRespondentEmail] = useState('');

  useEffect(() => {
    fetchInvestigationResponseContext(token)
      .then(setContext)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [token]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    if (recognizedTransaction == null || authorizedTransaction == null) {
      setError('Please answer both Yes/No questions.');
      return;
    }
    if (!explanation.trim() || !respondentName.trim() || !respondentEmail.trim()) {
      setError('Please complete all required fields.');
      return;
    }

    setSubmitting(true);
    try {
      const receipt = await submitInvestigationResponse(token, {
        recognizedTransaction,
        authorizedTransaction,
        explanation,
        respondentName,
        respondentEmail,
      });
      setSuccess(receipt);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <div className="page-alert-detail"><div className="alerts-loading">Loading secure response form…</div></div>;
  }

  if (success) {
    return (
      <div className="page-alert-detail">
        <div className="alert-detail-card">
          <h1 className="alert-detail-title">Response Received</h1>
          <p className="dismiss-modal__sub">{success.message}</p>
          <p className="alert-scope-hint">Receipt ID: #{success.responseId}</p>
        </div>
      </div>
    );
  }

  if (!context) {
    return <div className="page-alert-detail"><div className="alerts-empty">This secure link is invalid or unavailable.</div></div>;
  }

  if (context.alreadySubmitted) {
    return (
      <div className="page-alert-detail">
        <div className="alert-detail-card">
          <h1 className="alert-detail-title">Response Already Submitted</h1>
          <p className="dismiss-modal__sub">This secure link has already been used.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page-alert-detail">
      <div className="alert-detail-card">
        <h1 className="alert-detail-title">Verify Recent Activity</h1>
        <p className="dismiss-modal__sub">
          Please help us review recent account activity. Message reference #{context.messageId}.
        </p>

        {error && <p className="alert-action-error">{error}</p>}

        <form className="investigation-customer-form" onSubmit={handleSubmit}>
          <div className="investigation-question-block">
            <label className="dismiss-modal__label">Do you recognize this transaction?</label>
            <div className="investigation-inline-options">
              <label><input type="radio" name="recognized" checked={recognizedTransaction === true} onChange={() => setRecognizedTransaction(true)} /> Yes</label>
              <label><input type="radio" name="recognized" checked={recognizedTransaction === false} onChange={() => setRecognizedTransaction(false)} /> No</label>
            </div>
          </div>

          <div className="investigation-question-block">
            <label className="dismiss-modal__label">Did you authorize this transaction?</label>
            <div className="investigation-inline-options">
              <label><input type="radio" name="authorized" checked={authorizedTransaction === true} onChange={() => setAuthorizedTransaction(true)} /> Yes</label>
              <label><input type="radio" name="authorized" checked={authorizedTransaction === false} onChange={() => setAuthorizedTransaction(false)} /> No</label>
            </div>
          </div>

          <label className="dismiss-modal__label" htmlFor="explanation">Explanation</label>
          <textarea
            id="explanation"
            className="dismiss-modal__textarea"
            rows={5}
            value={explanation}
            onChange={e => setExplanation(e.target.value)}
            required
          />

          <label className="dismiss-modal__label" htmlFor="respondent-name">Your Name</label>
          <input
            id="respondent-name"
            className="investigation-input"
            value={respondentName}
            onChange={e => setRespondentName(e.target.value)}
            required
          />

          <label className="dismiss-modal__label" htmlFor="respondent-email">Your Email</label>
          <input
            id="respondent-email"
            type="email"
            className="investigation-input"
            value={respondentEmail}
            onChange={e => setRespondentEmail(e.target.value)}
            required
          />

          <div className="dismiss-modal__buttons">
            <button className="btn btn--secondary" type="submit" disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit Response'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

import React, { useState, useEffect, useCallback } from 'react';

const AiSummaryCard = ({ title, featureKey, fetchSummary, regenerateSummary }) => {
  const [summaryData, setSummaryData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [regenerating, setRegenerating] = useState(false);
  const [displayText, setDisplayText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const loadSummary = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetchSummary();
      if (response?.success && response?.data) {
        setSummaryData(response.data);
        typeText(response.data.summaryText || '');
      }
    } catch (err) {
      console.error('Error loading AI summary:', err);
    } finally {
      setLoading(false);
    }
  }, [fetchSummary]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  const typeText = (text) => {
    if (!text) return;
    setDisplayText('');
    setIsTyping(true);

    let index = 0;
    const interval = setInterval(() => {
      if (index < text.length) {
        setDisplayText(text.substring(0, index + 1));
        index++;
      } else {
        clearInterval(interval);
        setIsTyping(false);
      }
    }, 15);

    return () => clearInterval(interval);
  };

  const handleRegenerate = async () => {
    setRegenerating(true);
    try {
      const response = await regenerateSummary();
      if (response?.success && response?.data) {
        setSummaryData(response.data);
        typeText(response.data.summaryText || '');
      }
    } catch (err) {
      console.error('Error regenerating AI summary:', err);
    } finally {
      setRegenerating(false);
    }
  };

  return (
    <div className="ai-summary-card">
      <div className="ai-summary-header">
        <div className="ai-summary-title">
          <span>🤖</span>
          <span>{title || 'AI Analysis'}</span>
        </div>
        <button
          className="btn btn-outline btn-sm"
          onClick={handleRegenerate}
          disabled={regenerating || loading}
        >
          {regenerating ? '...' : '↻'} Regenerate
        </button>
      </div>

      <div className="ai-summary-content">
        {loading ? (
          <div className="loading-container">
            <div className="loading-spinner"></div>
          </div>
        ) : (
          <div className="ai-summary-typing">
            {displayText}
            {isTyping && <span className="ai-summary-cursor"></span>}
          </div>
        )}
      </div>

      <div className="ai-summary-footer">
        <span>Powered by Grok LLM · AETRIX India Intelligence Engine</span>
        {summaryData?.generatedAt && (
          <span>
            {summaryData.fromCache ? 'Cached' : 'Generated'}{' '}
            {new Date(summaryData.generatedAt).toLocaleTimeString()}
          </span>
        )}
      </div>
    </div>
  );
};

export default AiSummaryCard;

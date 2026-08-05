/**
 * networkUtils.js
 *
 * Shared helpers for the Network Insights page components.
 */
export function riskColor(score) {
  if (score === null || score === undefined) return '#ffffff';
  if (score >= 75) return '#ff4c4c';
  if (score >= 60) return '#ffcc00';
  if (score >= 40) return '#b38f00';
  return '#00ff88';
}

export function riskBucket(score) {
  if (score === null || score === undefined) return 'Unknown';
  if (score >= 75) return 'High';
  if (score >= 60) return 'Elevated';
  if (score >= 40) return 'Guarded';
  return 'Low';
}

export function formatTimestamp(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value).replace('T', ' ').slice(0, 19);
  }
  return date.toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

export function freshnessLabel(value) {
  if (!value) return 'Unknown';
  const then = new Date(value);
  if (Number.isNaN(then.getTime())) return 'Unknown';
  const ageHours = (Date.now() - then.getTime()) / (1000 * 60 * 60);
  if (ageHours <= 6) return 'Fresh';
  if (ageHours <= 24) return 'Aging';
  return 'Stale';
}

export function parseEvidence(rawEvidence) {
  if (!rawEvidence) return {};
  try {
    return JSON.parse(rawEvidence);
  } catch {
    return {};
  }
}

export function summarizeTopSignals(evidence, latest) {
  const bullets = [];
  const fraudNeighbors = evidence.confirmed_fraud_neighbor_count ?? 0;
  const sharedPayees = evidence.shared_payees ?? latest?.sharedPayeeCount ?? 0;
  const communitySize = evidence.community_size ?? latest?.communitySize ?? 0;
  const growthPercentile = evidence.growth_percentile ?? latest?.growthScore ?? 0;
  const exposurePercentile = evidence.page_rank_percentile ?? latest?.fraudExposureScore ?? 0;

  if (fraudNeighbors > 0 && exposurePercentile >= 50) {
    bullets.push(`Direct exposure to ${fraudNeighbors} confirmed-fraud neighbor(s).`);
  }
  if (sharedPayees >= 3) {
    bullets.push(`Shares ${sharedPayees} payee relationship(s) with nearby accounts.`);
  }
  if (communitySize >= 5) {
    bullets.push(`Sits inside a dense community of ${communitySize} structurally linked accounts.`);
  }
  if (growthPercentile >= 75) {
    bullets.push(`Recent payee growth is unusually high (${Math.round(growthPercentile)}th percentile).`);
  }
  if (bullets.length === 0 && exposurePercentile) {
    bullets.push(`Network exposure percentile is ${Math.round(exposurePercentile)}.`);
  }
  if (bullets.length === 0) {
    bullets.push('No standout network signals were recorded in this run.');
  }
  return bullets.slice(0, 2);
}

export function deriveRedFlags(evidence, latest) {
  const flags = [];
  if (latest?.networkRiskScore >= 75) flags.push('High overall risk');
  if ((evidence.confirmed_fraud_neighbor_count ?? 0) > 0) flags.push('Fraud-linked neighbor');
  if (evidence.is_dense_community) flags.push('Dense community');
  if ((evidence.growth_percentile ?? 0) >= 75) flags.push('Rapid growth');
  if ((latest?.sharedPayeeCount ?? evidence.shared_payees ?? 0) >= 5) flags.push('Heavy shared-payee overlap');
  return flags;
}

export function trendSummary(timeline) {
  if (!timeline || timeline.length === 0) {
    return {
      runCount: 0,
      latestScore: null,
      previousScore: null,
      delta: null,
      direction: 'none',
      summary: 'No historical run data available.',
    };
  }

  const latest = Number(timeline[timeline.length - 1].networkRiskScore ?? 0);
  const previous = timeline.length > 1 ? Number(timeline[timeline.length - 2].networkRiskScore ?? 0) : null;
  const delta = previous == null ? null : Number((latest - previous).toFixed(2));
  const direction = delta == null ? 'none' : delta > 0 ? 'up' : delta < 0 ? 'down' : 'flat';

  if (timeline.length < 2) {
    return {
      runCount: timeline.length,
      latestScore: latest,
      previousScore: previous,
      delta,
      direction,
      summary: 'Insufficient run history to establish a trend.',
    };
  }

  const signedDelta = delta > 0 ? `+${delta}` : `${delta}`;
  return {
    runCount: timeline.length,
    latestScore: latest,
    previousScore: previous,
    delta,
    direction,
    summary: `Risk changed ${signedDelta} over the last ${timeline.length} run(s).`,
  };
}

export function dominantSignalLabel(row, evidence = null) {
  const growth = Number(row?.growthScore ?? evidence?.growth_percentile ?? 0);
  const exposure = Number(row?.fraudExposureScore ?? evidence?.page_rank_percentile ?? 0);
  const shared = Number(row?.sharedPayeeCount ?? evidence?.shared_payees ?? 0);

  if (exposure >= growth && exposure >= shared) return 'Exposure-led';
  if (growth >= exposure && growth >= shared) return 'Growth-led';
  return 'Linkage-led';
}

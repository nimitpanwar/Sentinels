export default function OverviewLineChart({ points }) {
  if (!points.length) {
    return <div className="loading-state">No recent alert activity available for the chart.</div>;
  }

  const width = 640;
  const height = 240;
  const padding = 28;
  const maxValue = Math.max(...points.map(point => point.value), 1);

  const coordinates = points.map((point, index) => {
    const x = padding + ((width - padding * 2) * index) / Math.max(points.length - 1, 1);
    const y = height - padding - ((height - padding * 2) * point.value) / maxValue;
    return { ...point, x, y };
  });

  const path = coordinates.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ');
  const area = `${path} L ${coordinates[coordinates.length - 1].x} ${height - padding} L ${coordinates[0].x} ${height - padding} Z`;

  return (
    <div className="overview-chart-wrap">
      <svg className="overview-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Alert creation trend line chart">
        <defs>
          <linearGradient id="overviewArea" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="rgba(0,255,136,0.35)" />
            <stop offset="100%" stopColor="rgba(0,255,136,0.03)" />
          </linearGradient>
        </defs>
        <path d={area} fill="url(#overviewArea)" />
        <path d={path} fill="none" stroke="#00ff88" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
        {coordinates.map((point) => (
          <g key={point.label}>
            <circle cx={point.x} cy={point.y} r="4.5" fill="#33ffa1" />
            <text x={point.x} y={height - 8} textAnchor="middle" className="overview-chart__label">{point.label}</text>
          </g>
        ))}
      </svg>
    </div>
  );
}

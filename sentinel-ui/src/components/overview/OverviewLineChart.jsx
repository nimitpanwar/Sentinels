/**
 * OverviewLineChart.jsx
 *
 * Small, dependency-free SVG line graph (same "no charting library" approach
 * as NetworkGraphView.jsx) showing a daily count series as a line + filled
 * area, with light gridlines and a handful of date labels along the x-axis.
 */
export default function OverviewLineChart({ data }) {
  const W = 640;
  const H = 220;
  const PAD_X = 28;
  const PAD_Y = 20;

  if (!data || data.length === 0) {
    return <div className="overview-chart-empty">No data yet.</div>;
  }

  const max = Math.max(1, ...data.map((d) => d.count));
  const innerW = W - PAD_X * 2;
  const innerH = H - PAD_Y * 2;
  const stepX = data.length > 1 ? innerW / (data.length - 1) : 0;

  const points = data.map((d, i) => ({
    ...d,
    x: PAD_X + i * stepX,
    y: PAD_Y + innerH - (d.count / max) * innerH,
  }));

  const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
  const areaPath = `${linePath} L ${points[points.length - 1].x} ${PAD_Y + innerH} L ${points[0].x} ${PAD_Y + innerH} Z`;

  const labelIdxs = new Set([0, Math.floor((points.length - 1) / 2), points.length - 1]);

  // date is 'YYYY-MM-DD' -> display as 'DD-MM'
  const formatLabel = (date) => {
    const [, month, day] = date.split('-');
    return `${day}-${month}`;
  };

  return (
    <svg className="overview-chart-svg" viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none">
      {[0, 0.5, 1].map((f) => {
        const y = PAD_Y + innerH - f * innerH;
        return <line key={f} className="overview-chart-grid" x1={PAD_X} x2={W - PAD_X} y1={y} y2={y} />;
      })}

      <path className="overview-chart-area" d={areaPath} />
      <path className="overview-chart-line" d={linePath} />

      {points.map((p) => (
        <circle key={p.date} className="overview-chart-dot" cx={p.x} cy={p.y} r={3}>
          <title>{`${p.date}: ${p.count} handled`}</title>
        </circle>
      ))}

      {points.map((p, i) => (
        labelIdxs.has(i) && (
          <text key={p.date} className="overview-chart-label" x={p.x} y={H - 2} textAnchor="middle">
            {formatLabel(p.date)}
          </text>
        )
      ))}
    </svg>
  );
}

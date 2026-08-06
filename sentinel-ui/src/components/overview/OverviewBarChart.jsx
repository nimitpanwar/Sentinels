export default function OverviewBarChart({ bars }) {
  const max = Math.max(...bars.map(b => b.value), 1);
  return (
    <div className="overview-bar-chart">
      {bars.map(bar => (
        <div key={bar.label} className="overview-bar-chart__row">
          <span className="overview-bar-chart__label">{bar.label}</span>
          <div className="overview-bar-chart__track">
            <div
              className="overview-bar-chart__fill"
              style={{
                width: `${Math.round((bar.value / max) * 100)}%`,
                background: bar.color || '#00ff88',
              }}
            />
          </div>
          <span className="overview-bar-chart__count mono">{bar.value}</span>
        </div>
      ))}
    </div>
  );
}

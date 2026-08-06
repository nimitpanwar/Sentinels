export default function OverviewHeatmap({ cells }) {
  const max = Math.max(...cells.map(c => c.value), 1);
  const cellW = 22;
  const cellH = 22;
  const gap = 3;
  const totalW = 24 * (cellW + gap) - gap;

  return (
    <div className="overview-heatmap-wrap">
      <div className="overview-heatmap__eyebrow">Hourly activity — 00:00 to 23:00</div>
      <svg
        viewBox={`0 0 ${totalW} ${cellH + 18}`}
        className="overview-heatmap"
        role="img"
        aria-label="Hourly alert activity heatmap"
      >
        {cells.map((cell, i) => {
          const x = i * (cellW + gap);
          const opacity = cell.value === 0 ? 0.08 : 0.2 + (cell.value / max) * 0.8;
          return (
            <g key={cell.hour}>
              <rect
                x={x}
                y={0}
                width={cellW}
                height={cellH}
                rx={4}
                fill="#00ff88"
                fillOpacity={opacity}
              />
              {cell.hour % 6 === 0 && (
                <text
                  x={x + cellW / 2}
                  y={cellH + 13}
                  textAnchor="middle"
                  className="overview-chart__label"
                >
                  {String(cell.hour).padStart(2, '0')}h
                </text>
              )}
            </g>
          );
        })}
      </svg>
    </div>
  );
}

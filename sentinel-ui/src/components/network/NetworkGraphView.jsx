/**
 * NetworkGraphView.jsx
 *
 * Small, deterministic (no physics simulation, no external graph library)
 * local neighborhood view: the selected account in the center, its
 * shared-payee neighbors arranged evenly around it in a circle, edge
 * thickness proportional to shared-payee count, node color by network risk
 * score. Deliberately scoped to ~20-30 nodes (see NetworkController) rather
 * than rendering the whole network - investigators rarely need the full
 * graph, just the immediate neighborhood.
 */
import { riskColor } from '../../utils/networkUtils';

const WIDTH = 320;
const HEIGHT = 280;
const CENTER_X = WIDTH / 2;
const CENTER_Y = HEIGHT / 2;
const RADIUS = 105;

export default function NetworkGraphView({ graph, onSelectNode, showLabels = true }) {
  if (!graph || graph.nodes.length === 0) {
    return <div className="loading-state">No neighborhood data for this account.</div>;
  }

  const center = graph.nodes.find((n) => n.isCenter) || graph.nodes[0];
  const neighbors = graph.nodes.filter((n) => !n.isCenter);

  const positioned = neighbors.map((node, i) => {
    const angle = (i / Math.max(neighbors.length, 1)) * 2 * Math.PI - Math.PI / 2;
    return {
      ...node,
      x: CENTER_X + RADIUS * Math.cos(angle),
      y: CENTER_Y + RADIUS * Math.sin(angle),
    };
  });

  const edgeByNeighbor = Object.fromEntries(
    graph.edges.map((e) => [e.targetAccountId === center.accountId ? e.sourceAccountId : e.targetAccountId, e])
  );

  return (
    <svg className="network-graph-svg" viewBox={`0 0 ${WIDTH} ${HEIGHT}`}>
      {/* Edges (drawn first, under the nodes) */}
      {positioned.map((n) => {
        const edge = edgeByNeighbor[n.accountId];
        const weight = edge ? Math.min(1 + Math.log1p(edge.sharedPayeeCount), 6) : 1;
        return (
          <line
            key={`edge-${n.accountId}`}
            x1={CENTER_X}
            y1={CENTER_Y}
            x2={n.x}
            y2={n.y}
            stroke="#cbd5e1"
            strokeWidth={weight}
          />
        );
      })}

      {/* Neighbor nodes */}
      {positioned.map((n) => (
        <g key={`node-${n.accountId}`} onClick={() => onSelectNode?.(n.accountId)} style={{ cursor: 'pointer' }}>
          <circle cx={n.x} cy={n.y} r={9} fill={riskColor(n.networkRiskScore)} stroke="#fff" strokeWidth={1.5} />
          {showLabels && (
            <text x={n.x} y={n.y + 22} textAnchor="middle" fontSize="9" fill="#374151">
              {n.accountNumber || `#${n.accountId}`}
            </text>
          )}
        </g>
      ))}

      {/* Center node (drawn last, on top) */}
      <circle cx={CENTER_X} cy={CENTER_Y} r={15} fill="#1e40af" stroke="#fff" strokeWidth={2} />
      {showLabels && (
        <text x={CENTER_X} y={CENTER_Y + 30} textAnchor="middle" fontSize="10" fontWeight="600" fill="#111827">
          {center.accountNumber || `#${center.accountId}`}
        </text>
      )}
    </svg>
  );
}

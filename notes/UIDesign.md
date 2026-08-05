# Design Specification: Sentinel Command Center

## Project Overview
**Name:** Sentinel
**Industry:** Fintech / Cybersecurity
**User Persona:** High-stakes Operations Specialist (Alert Triage & Investigation)
**Core Aesthetic:** High-density, data-driven "Command Center" interface.
### 1. Color Palette
The interface uses a strict, low-luminance palette to reduce eye strain during long shifts while maintaining high contrast for critical information.

- **Primary Background:** `#121317` (Deep Charcoal/Black)
- **Secondary Background/Surfaces:** `#1a1b1f` (Containers), `#0d0e12` (Deepest Black)
- **Primary Text:** `#ffffff` (White)
- **Secondary Text (Dim):`#8a8d91`
- **Critical Action/Alert:** `#ff4c4c` (Vivid Red)
- **Success/Operational:** `#00ff88` (Neon Mint)
- **Warning/Pending:** `#ffcc00` (Signal Amber)
- **Borders/Outlines:** `#38393d`

### 2. Typography
- **Primary Typeface:** `Inter` (or similar clean sans-serif)
- **Data/ID Typeface:** Monospaced (e.g., `JetBrains Mono` or `Roboto Mono`) for transaction IDs, timestamps, and hash values.
- **Hierarchy:**
  - **Headings:** All-caps, tracked-out (+0.05em), bold.
  - **Status Labels:** Small-caps or all-caps with subtle backgrounds.

### 3. Spacing & Borders
- **Border Radius:** `2px` (Sharp, industrial feel)
- **Border Weight:** `1px`
- **Layout:** High-density grid with fixed sidebars and modular "panes."

---

## UI Components & Patterns

### 1. Navigation Shell
- **Side Navigation:** Vertical, minimal icons with labels. Houses "Dashboard", "Alerts", "Investigate", and "Analytics".
- **Global Status Bar:** Fixed at the bottom, showing system health (e.g., "CORE_SERVER: OPERATIONAL") and timestamp.

### 2. Modular Panes (The Dashboard)
- **KPI Tiles:** Condensed headers with large numerical values and secondary trend indicators (e.g., "SYS_HEALTH 99.98% +0.02%").
- **Real-Time Stream:** A high-density table. Rows alternate between dim and bright text based on alert severity.
- **Visual Pulse:** Sparkline or waveform charts showing system latency and error rates. Use `#ff4c4c` for error lines.

### 3. Investigation Sidebar
- **Context Drawer:** Slides in from the right to provide metadata on a selected alert.
- **Action Group:** Large, high-contrast buttons for triage (e.g., "QUARANTINE" in red, "DISMISS" in secondary outline).

---


To replicate this in your project:
1. **Global CSS:** Define the dark-mode tokens as CSS variables.
2. **Layout Engine:** Use a `flex` or `grid` shell with a `h-screen overflow-hidden` container to maintain the "cockpit" feel.
3. **Data Visualization:** Use a charting library (like Recharts or D3) with zero-interpolation (step) lines to match the digital aesthetic.
4. **Tailwind Mapping:**
   - Backgrounds: `bg-[#121317]`
   - Borders: `border-[#38393d]`
   - Accents: `text-[#ff4c4c]` (Red), `text-[#00ff88]` (Green)
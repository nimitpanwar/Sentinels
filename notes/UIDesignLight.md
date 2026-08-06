# Design Specification: Sentinel Command Center (Light Theme)

## Project Overview
**Name:** Sentinel
**Industry:** Fintech / Cybersecurity
**User Persona:** High-stakes Operations Specialist (Alert Triage & Investigation)
**Core Aesthetic:** High-density, high-contrast "Command Center" interface optimized for daytime operations. This is a light-themed counterpart to the original Sentinel dark mode.

---

## Design System Tokens (Light Mode)

### 1. Color Palette
A high-contrast utility palette designed for clarity and rapid data processing on light surfaces.

- **Primary Background:** `#FCF8F8` (Off-white / Paper)
- **Secondary Surfaces:** `#F7F3F2` (Subtle grey-tinted surfaces for containers)
- **Primary Text:** `#121317` (Deep Charcoal / Black)
- **Secondary Text (Dim):** `#666666`
- **Critical Action/Alert:** `#FF4C4C` (Vivid Red)
- **Success/Operational:** `#00AA66` (Deep Mint - adjusted for light mode contrast)
- **Warning/Pending:** `#E6B800` (Signal Amber - adjusted for light mode contrast)
- **Borders/Outlines:** `#DDD9D9` (Sharp, hair-line separators)

### 2. Typography
- **Primary Typeface:** `Inter`
- **Data/ID Typeface:** Monospaced (`JetBrains Mono` or `Roboto Mono`) for all system logs, transaction IDs, and timestamps.
- **Hierarchy:**
  - **Headings:** All-caps, bold, tracking +0.05em.
  - **Status Labels:** Small-caps with semantic background tints.

### 3. Interactive States
- **Hover (Action Center):** Buttons in the triage and action panels transition to a reddish tint (`#FF4C4C` at varying opacities or solid with white text) to signal danger/importance.
- **Active Navigation:** Selected sidebar items use a solid black indicator bar and bold weight.

---

## UI Components & Layout

### 1. Shell Structure
- **Global Layout:** `100vh` height with `overflow-hidden`. The screen is a fixed "cockpit" with three distinct columns.
- **Side Navigation (Left):** Minimalist vertical bar (~240px). Includes brand logo (`{{DATA:IMAGE:IMAGE_5}}`), main navigation tabs (Dashboard, Alerts, Investigate, Analytics), and footer links (Support, Logs).
- **Control Bar (Top):** Search bar for IPs/Users and global utility icons (Notifications, Settings, Profile).

### 2. Dashboard Panes
- **KPI Grid:** Top-row tiles for `SYS_HEALTH`, `ACTIVE_THREATS`, `TPS_CURRENT`, and `LATENCY_MS`.
- **System Pulse (Center-Left):** Real-time waveform visualization showing system activity. Uses `#121317` for the main line and light grey for historical traces.
- **Threat Map (Center-Right):** A detailed vector map overlay showing global threat vectors. High-contrast monochromatic styling.
- **Active Alerts Stream (Bottom):** High-density table with monospaced data. Severity columns are color-coded (CRITICAL = Red, WARNING = Amber, INFO = Grey).

### 3. Context Prism (Right Sidebar)
- **Focus Pane:** Displays detailed metadata for the currently selected alert entity (e.g., `TXN-99A8-F234`).
- **Action Center:** Primary triage controls:
  - **Quarantine:** High-contrast solid button (Red hover).
  - **Dismiss:** Outline button (Red hover).
  - **Emergency Shutdown:** Red-themed critical action button at the bottom.

---

## Technical Integration Guide (AI Copilot Reference)

### Tailwind Configuration
```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        'sentinel-bg': '#FCF8F8',
        'sentinel-surface': '#F7F3F2',
        'sentinel-text': '#121317',
        'sentinel-border': '#DDD9D9',
        'sentinel-red': '#FF4C4C',
        'sentinel-green': '#00AA66',
        'sentinel-amber': '#E6B800',
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    }
  }
}
```

### Component Logic
- **Hover Effects:** Apply `transition-colors duration-200 hover:bg-sentinel-red hover:text-white` to action buttons.
- **Table Density:** Use `py-1 px-2` for table rows to maintain "command center" density.
- **Layout:** Use `flex h-screen` for the main wrapper to ensure no vertical scrolling on the page level.

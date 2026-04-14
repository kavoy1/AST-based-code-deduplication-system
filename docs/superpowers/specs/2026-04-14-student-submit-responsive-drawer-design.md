---
title: Student Submit Page Responsive Drawer
date: 2026-04-14
status: draft
---

## Overview
When the app is windowed or narrowed, the student layout currently pushes the left sidebar below the main content, which breaks the page’s visual structure. We will replace the sidebar with a left-side drawer on small widths and expose a menu button in the student submit page title area to open it.

## Goals
- Remove the “sidebar drops to bottom” behavior in windowed/small widths.
- Provide a consistent, predictable navigation entry point in the student submit page header.
- Preserve the existing sidebar content and styling inside the drawer.
- Keep large-screen behavior unchanged.

## Non‑Goals
- Redesign the sidebar contents or nav labels.
- Change teacher/admin layouts.
- Add new navigation items or permissions.

## UX Behavior
- **Breakpoint**: at viewport width `<= 1180px`:
  - Student sidebar is hidden from the main layout grid.
  - A **Menu** button appears in the student submit page header (left side, same row as Back button).
  - Clicking the Menu button opens a **left drawer** containing the existing student sidebar.
  - Drawer closes on overlay click, Esc key, or re-clicking the menu button.
- At widths `> 1180px`: the sidebar remains visible as-is and the menu button is hidden.

## Visual/Interaction Details
- Menu button uses the existing `workspace-icon-button` style for visual consistency.
- Drawer width: **280px** (matches sidebar density but slightly more spacious for touch).
- Drawer background uses existing sidebar styles and spacing.
- Overlay uses a soft translucent background to keep focus on the drawer.

## Components / Files Affected
- **Layout**: student layout container (`frontend/src/views/Layout.vue`).
- **Student submit page**: header controls (`frontend/src/views/student/AssignmentSubmit.vue`).
- **CSS**: responsive behavior for the student shell (`frontend/src/style.css`).

## Data Flow
No changes to data fetching or API flow. Drawer open/close is purely UI state.

## Error Handling / Edge Cases
- If the drawer is open and the route changes, the drawer should close automatically.
- If the user resizes from small to large width, the drawer should close and the static sidebar should reappear.

## Testing
- Manual: resize window from large to small, confirm sidebar becomes drawer and layout remains stable.
- Manual: confirm menu button appears only at `<=1180px`.
- Manual: open/close drawer via button, overlay, and Esc.
- Manual: confirm large screen layout unchanged.

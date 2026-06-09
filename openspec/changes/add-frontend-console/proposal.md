## Why

The short-link service currently exposes complete backend APIs but has no first-party frontend for public link creation, authenticated management, analytics, recycle-bin operations, or API token management. A root-level React frontend will make the project usable as a complete product while keeping the existing short-link redirect path intact.

## What Changes

- Add a Vite-powered React + TypeScript frontend project under the repository root at `frontend/`.
- Build a public landing page that supports anonymous short-link creation and mirrors the clean structure of `https://link.chanler.dev`.
- Build authenticated console pages for login, registration, dashboard, groups, links, recycle bin, analytics, developer tokens, and profile settings.
- Integrate the frontend with the existing admin API contract and keep the core API token endpoints available for developer/API use.
- Add Spring Boot static routing for the SPA under `/app/**`, avoiding conflicts with `GET /{shortUri}` redirects.
- Add frontend build/typecheck scripts and production asset integration so the Java application can serve the compiled SPA.
- Add the minimal backend response fields needed for token management UI actions, specifically API token `id` and update timestamp.

## Capabilities

### New Capabilities

- `frontend-console`: Public short-link creation and authenticated management UI for links, groups, analytics, recycle-bin operations, API tokens, and profile settings.

### Modified Capabilities

- None.

## Impact

- New root-level `frontend/` project using React, TypeScript, Tailwind CSS, and Vite.
- Spring Boot web/static configuration for `/app` SPA fallback and compiled frontend assets.
- Existing admin endpoints under `/api/short-link/admin/v1/**` become the primary frontend API surface.
- Existing core endpoints under `/api/short-link/v1/**` remain available for API token consumers.
- Token list response shape needs `id` and `updateTime` so the UI can call delete and status-update endpoints reliably.
- Build and verification commands expand to include frontend typecheck/build and backend Maven packaging.

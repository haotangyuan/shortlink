# Comet Design Handoff

- Change: add-frontend-console
- Phase: design
- Mode: compact
- Context hash: 7ea0f148a8bebbf0573c5f27559a4828d81bdbf5264a1d82603b6b5c26aaf767

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-frontend-console/proposal.md

- Source: openspec/changes/add-frontend-console/proposal.md
- Lines: 1-32
- SHA256: 7f21a52e0e56ab2c2aec0d90972a097239c1f8fc688d0e6ea9f40dbee6712a0a

```md
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
```

## openspec/changes/add-frontend-console/design.md

- Source: openspec/changes/add-frontend-console/design.md
- Lines: 1-105
- SHA256: 679e176b1fe8433b31f860a463abd6deac4abbce6daa8a39f1c2fa7902b59dac

[TRUNCATED]

```md
## Context

The service is a Spring Boot 3 backend with two API surfaces:

- Admin APIs under `/api/short-link/admin/v1/**` use a session token returned by login.
- Core APIs under `/api/short-link/v1/**` use API tokens and are meant for programmatic access.

The backend also owns `GET /{shortUri}` for short-link redirects. A frontend that uses root-level browser routes such as `/dashboard` would collide with short-code routing. The frontend therefore needs a dedicated app prefix while still living in this repository for project management.

The reference site at `https://link.chanler.dev` provides the target product shape: public landing page, anonymous link creation, auth pages, dashboard shell, groups, links, analytics, recycle bin, profile, and developer token pages.

## Goals / Non-Goals

**Goals:**

- Add a root-level `frontend/` Vite app using React, TypeScript, and Tailwind CSS.
- Serve the production SPA from the Spring Boot app under `/app`.
- Preserve existing short-link redirect behavior at `/{shortUri}`.
- Implement page modules that map directly to existing backend controllers and DTO/VO shapes.
- Keep the frontend API layer typed and centralized so request fields stay aligned with backend contracts.
- Add the small backend response-field fix required for API token UI actions.

**Non-Goals:**

- Do not redesign the short-link generation algorithm, persistence model, rate limiting, or statistics ingestion.
- Do not add custom domains in the first frontend version because the backend currently ignores the create request `domain` field.
- Do not expose "permanent link" creation in the UI because the current backend forces created links to custom expiry with a maximum of three days.
- Do not replace the existing Swagger/Scalar API documentation.

## Decisions

### Use `frontend/` at the repository root

The frontend source will live at `frontend/`, beside `src/`, `pom.xml`, and `openspec/`. This matches the requested project layout and keeps Java and frontend source trees clearly separated.

Alternatives considered:

- Put frontend under `src/main/frontend`: closer to Java packaging, but harder to run independently.
- Use a separate repository: cleaner deployment boundary, but worse for this project because the user wants one manageable project.

### Serve the SPA under `/app`

The Vite router will use `basename="/app"`, and Spring Boot will forward `/app` and `/app/**` to the compiled SPA entrypoint. This avoids conflicts with `GET /{shortUri}`.

Alternatives considered:

- Root routes such as `/login` and `/dashboard`: cleaner URLs, but unsafe because they can be interpreted as short codes.
- Hash routes such as `/#/dashboard`: avoids backend routing changes, but creates weaker URLs and poorer product polish.

### Use the admin API for the console and keep core API for developers

The authenticated console will call `/api/short-link/admin/v1/**` using `Authorization: Bearer <session-token>`. The developer center will create/list/revoke API tokens, and documentation snippets can show how to use `/api/short-link/v1/**` with those API tokens.

Alternatives considered:

- Use core APIs from the console after creating an API token: unnecessary and would make normal console usage harder.
- Add a new frontend-specific backend API: more work and duplication without current need.

### Keep UI fields honest to backend behavior

The create/edit forms will hide unsupported or misleading controls:

- No custom domain field in the first version.
- No permanent-validity option in create forms.
- Expiry copy will state that the backend caps link validity at three days.

The UI can still submit `validDateType` and `validDate` in the backend-required shape.

### Use typed request modules instead of generated clients

The project has Springdoc but only exposes `paths-to-match: /api/short-link/v1/**`, so admin endpoints are not fully covered by OpenAPI output. The first implementation will define TypeScript request/response types manually from the Java DTO/VO classes and centralize calls in `frontend/src/api`.

Alternatives considered:

- Generate a client from OpenAPI: currently incomplete for admin APIs.
- Inline `fetch` in pages: faster initially, but makes it easier for UI and backend contracts to drift.

### Add TokenVO `id` and `updateTime`

`DELETE /api/short-link/admin/v1/token/{id}` and `PATCH /token/{id}/status` require token IDs. The list response currently exposes `tokenMasked`, `name`, `enableStatus`, `validDate`, and `describe`, but not `id`. The frontend cannot reliably manage tokens without `id`; `updateTime` is also needed for the token table shown in the reference design.
```

Full source: openspec/changes/add-frontend-console/design.md

## openspec/changes/add-frontend-console/tasks.md

- Source: openspec/changes/add-frontend-console/tasks.md
- Lines: 1-58
- SHA256: 66f2cedbcf218c7a77b16c0e6b9421c1260fb636cf62612e9ee263d03f0bbbaa

```md
## 1. Frontend Project Setup

- [ ] 1.1 Create the root-level `frontend/` Vite React TypeScript project structure.
- [ ] 1.2 Add Tailwind CSS, router, query, form, validation, chart, and icon dependencies.
- [ ] 1.3 Configure Vite dev proxy for `/api` to `http://127.0.0.1:8068`.
- [ ] 1.4 Configure React Router with `basename="/app"` and shared app providers.
- [ ] 1.5 Add shared UI primitives, layout tokens, and utility helpers.

## 2. Backend Integration Support

- [ ] 2.1 Add Spring Boot SPA forwarding for `/app` and `/app/**` without affecting `/{shortUri}`.
- [ ] 2.2 Add production static asset placement for the Vite build output.
- [ ] 2.3 Add `id` and `updateTime` to the token list response object and service mapping.
- [ ] 2.4 Document root-level frontend build and backend package commands.

## 3. API Client And Auth State

- [ ] 3.1 Define TypeScript request and response types for the admin and core API contracts.
- [ ] 3.2 Implement a centralized result-envelope unwrapping fetch client.
- [ ] 3.3 Implement admin session token storage, hydration, validation, and logout handling.
- [ ] 3.4 Implement route guards for authenticated dashboard routes.
- [ ] 3.5 Add consistent loading, empty, validation, unauthorized, rate-limited, and backend-error handling utilities.

## 4. Public And Auth Pages

- [ ] 4.1 Build the public landing page and anonymous short-link creation form.
- [ ] 4.2 Build short-link creation success and copy/open result states.
- [ ] 4.3 Build login page with password visibility toggle and error handling.
- [ ] 4.4 Build registration page with backend-aligned fields and validation.

## 5. Console Shell And Dashboard

- [ ] 5.1 Build the authenticated dashboard shell with responsive sidebar, header, and user controls.
- [ ] 5.2 Build dashboard aggregate cards and quick actions.
- [ ] 5.3 Build dashboard group overview and empty states.

## 6. Management Modules

- [ ] 6.1 Build group management list, create, rename, delete, and sort interactions.
- [ ] 6.2 Build link list with group filter, search, pagination, copy, open, analytics, edit, and recycle actions.
- [ ] 6.3 Build link create form with backend-supported validity behavior and no unsupported custom-domain control.
- [ ] 6.4 Build link edit form with origin group, target group, URL, description, and validity fields.
- [ ] 6.5 Build recycle bin list, restore, and permanent-remove interactions.

## 7. Analytics And Developer Modules

- [ ] 7.1 Build analytics filters for group, optional link, date range, and quick ranges.
- [ ] 7.2 Build PV, UV, UIP summary cards and trend charts.
- [ ] 7.3 Build browser, OS, device, network, locale, visitor type, top IP, and access-record sections.
- [ ] 7.4 Build API token list, create, plaintext-once display, status toggle, copy, and revoke interactions.
- [ ] 7.5 Build profile settings view and update flow.

## 8. Verification

- [ ] 8.1 Run frontend typecheck and production build.
- [ ] 8.2 Run backend Maven package with frontend assets included.
- [ ] 8.3 Run browser smoke tests for public create, login, dashboard route guard, group management, link management, recycle bin, analytics, token management, and profile settings.
- [ ] 8.4 Verify `/app/**` refreshes serve the SPA and `/{shortUri}` remains handled by the redirect controller.
```

## openspec/changes/add-frontend-console/specs/frontend-console/spec.md

- Source: openspec/changes/add-frontend-console/specs/frontend-console/spec.md
- Lines: 1-152
- SHA256: f989bdf0b4e91a19ba1ede5c45a92ce80dcf9124c894352a540a8a7017a8a06b

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: SPA Routing Isolation
The system SHALL serve the frontend console under `/app` and MUST preserve `GET /{shortUri}` for short-link redirects.

#### Scenario: App route is served
- **WHEN** a browser requests `/app/dashboard/links`
- **THEN** the system serves the frontend SPA entrypoint instead of treating `dashboard` or `links` as short-link codes

#### Scenario: Short-link redirect remains available
- **WHEN** a browser requests `/{shortUri}` outside the `/app` prefix
- **THEN** the existing short-link redirect flow handles the request

### Requirement: Root-Level Frontend Project
The repository SHALL contain a root-level `frontend/` project using React, TypeScript, Tailwind CSS, and Vite.

#### Scenario: Frontend source is isolated
- **WHEN** a developer opens the repository root
- **THEN** frontend source, scripts, and configuration are located under `frontend/`

#### Scenario: Frontend build is reproducible
- **WHEN** a developer runs the documented frontend build command
- **THEN** Vite produces production assets for Spring Boot to serve under `/app`

### Requirement: Public Landing And Anonymous Link Creation
The frontend SHALL provide a public landing page that allows anonymous users to create a short link without logging in.

#### Scenario: Anonymous link creation succeeds
- **WHEN** an anonymous user submits a valid allowed URL and optional description
- **THEN** the frontend calls the anonymous create endpoint without an Authorization header and displays the returned short link with copy controls

#### Scenario: Anonymous link creation fails
- **WHEN** the backend rejects the URL, rate limits the request, or returns a non-success result code
- **THEN** the frontend displays the backend error message and keeps the submitted URL available for correction

### Requirement: Authentication Flow
The frontend SHALL support registration, login, session persistence, session validation, and logout using admin user APIs.

#### Scenario: User logs in
- **WHEN** a user submits valid username and password credentials
- **THEN** the frontend stores the returned session token and loads the user's profile before showing authenticated pages

#### Scenario: Protected route requires login
- **WHEN** an unauthenticated user opens an `/app/dashboard/**` route
- **THEN** the frontend redirects the user to `/app/login`

#### Scenario: User logs out
- **WHEN** a logged-in user chooses logout
- **THEN** the frontend calls the logout API, clears local auth state, and returns the user to the public page

### Requirement: Dashboard Overview
The frontend SHALL provide an authenticated dashboard with aggregate link, group, and visit metrics.

#### Scenario: Dashboard loads metrics
- **WHEN** a logged-in user opens `/app/dashboard`
- **THEN** the frontend loads groups and available group statistics and displays total links, group count, today's visits, and total visits

#### Scenario: Dashboard has no groups
- **WHEN** the user has no groups
- **THEN** the frontend displays an empty state with actions to create a group or create a link

### Requirement: Group Management
The frontend SHALL allow authenticated users to create, view, rename, delete, and sort short-link groups.

#### Scenario: Group list is shown
- **WHEN** a logged-in user opens the group management page
- **THEN** the frontend displays group name, gid, sort order, and link count for each group

#### Scenario: Group is renamed
- **WHEN** the user submits a new group name for an existing gid
- **THEN** the frontend calls the group update API and refreshes the group list after success

### Requirement: Link Management
The frontend SHALL allow authenticated users to create, batch create, list, search, copy, open, edit, and move links to the recycle bin.

#### Scenario: Link list is filtered by group
- **WHEN** a user selects a group on the link management page
- **THEN** the frontend calls the link page API with that gid and displays paginated link records

#### Scenario: Link is moved to recycle bin
```

Full source: openspec/changes/add-frontend-console/specs/frontend-console/spec.md


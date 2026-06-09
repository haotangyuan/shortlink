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
- **WHEN** the user confirms moving a link to the recycle bin
- **THEN** the frontend calls the recycle-bin save API with gid and fullShortUrl and removes the link from the active list after success

#### Scenario: Link create form respects backend validity behavior
- **WHEN** a user creates a link
- **THEN** the frontend submits the backend-supported validity fields and does not present unsupported permanent-validity or custom-domain controls

### Requirement: Recycle Bin Management
The frontend SHALL allow authenticated users to view recycled links across selected groups, restore links, and permanently remove links.

#### Scenario: Recycle bin list is shown
- **WHEN** a user opens the recycle bin page
- **THEN** the frontend calls the recycle-bin page API with selected gidList values and displays paginated disabled links

#### Scenario: Recycled link is restored
- **WHEN** the user restores a recycled link
- **THEN** the frontend calls the restore API and moves the link out of the recycle bin view after success

### Requirement: Analytics
The frontend SHALL provide group-level and single-link analytics using the existing stats APIs.

#### Scenario: Group analytics are displayed
- **WHEN** a user selects a group and date range
- **THEN** the frontend displays PV, UV, UIP, trend, browser, OS, device, network, locale, visitor type, top IP, and access-record data from group stats APIs

#### Scenario: Single-link analytics are displayed
- **WHEN** a user selects a specific link within a group
- **THEN** the frontend displays stats and access records for that fullShortUrl and gid

### Requirement: Developer Token Management
The frontend SHALL allow authenticated users to create, list, enable, disable, copy, and revoke API tokens.

#### Scenario: Token is created
- **WHEN** a user creates an API token with name, optional description, and optional expiry
- **THEN** the frontend displays the plaintext token once and instructs the user to save it

#### Scenario: Token status changes
- **WHEN** a user toggles token status
- **THEN** the frontend calls the token status API using the token id and refreshes the token list

### Requirement: Profile Settings
The frontend SHALL allow authenticated users to view and update their profile information using admin user APIs.

#### Scenario: Profile is loaded
- **WHEN** a logged-in user opens profile settings
- **THEN** the frontend loads username, real name, phone, and email into an editable form

#### Scenario: Profile is updated
- **WHEN** a user submits valid profile changes
- **THEN** the frontend calls the user update API and refreshes local user state after success

### Requirement: API Error And Loading States
The frontend SHALL handle success, loading, empty, validation, unauthorized, rate-limited, and backend error states consistently.

#### Scenario: Unauthorized API response
- **WHEN** an authenticated API request returns HTTP 401 or a token failure result
- **THEN** the frontend clears auth state and redirects the user to `/app/login`

#### Scenario: Backend business error
- **WHEN** an API response has a non-success result code
- **THEN** the frontend displays the response message without treating the request as successful

### Requirement: Responsive Console UI
The frontend SHALL provide responsive layouts for public pages and dashboard modules.

#### Scenario: Mobile dashboard navigation
- **WHEN** a user opens the dashboard on a narrow viewport
- **THEN** navigation, tables, forms, and chart areas remain usable without overlapping text or controls

#### Scenario: Desktop dashboard navigation
- **WHEN** a user opens the dashboard on a desktop viewport
- **THEN** the sidebar, content header, tables, cards, and chart sections align consistently with the reference product structure

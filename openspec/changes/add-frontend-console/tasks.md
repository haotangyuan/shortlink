## 1. Frontend Project Setup

- [x] 1.1 Create the root-level `frontend/` Vite React TypeScript project structure.
- [x] 1.2 Add Tailwind CSS, router, query, form, validation, chart, and icon dependencies.
- [x] 1.3 Configure Vite dev proxy for `/api` to `http://127.0.0.1:8068`.
- [x] 1.4 Configure React Router with `basename="/app"` and shared app providers.
- [x] 1.5 Add shared UI primitives, layout tokens, and utility helpers.

## 2. Backend Integration Support

- [x] 2.1 Add Spring Boot SPA forwarding for `/app` and `/app/**` without affecting `/{shortUri}`.
- [x] 2.2 Add production static asset placement for the Vite build output.
- [x] 2.3 Add `id` and `updateTime` to the token list response object and service mapping.
- [x] 2.4 Document root-level frontend build and backend package commands.

## 3. API Client And Auth State

- [x] 3.1 Define TypeScript request and response types for the admin and core API contracts.
- [x] 3.2 Implement a centralized result-envelope unwrapping fetch client.
- [x] 3.3 Implement admin session token storage, hydration, validation, and logout handling.
- [x] 3.4 Implement route guards for authenticated dashboard routes.
- [x] 3.5 Add consistent loading, empty, validation, unauthorized, rate-limited, and backend-error handling utilities.

## 4. Public And Auth Pages

- [x] 4.1 Build the public landing page and anonymous short-link creation form.
- [x] 4.2 Build short-link creation success and copy/open result states.
- [x] 4.3 Build login page with password visibility toggle and error handling.
- [x] 4.4 Build registration page with backend-aligned fields and validation.

## 5. Console Shell And Dashboard

- [x] 5.1 Build the authenticated dashboard shell with responsive sidebar, header, and user controls.
- [x] 5.2 Build dashboard aggregate cards and quick actions.
- [x] 5.3 Build dashboard group overview and empty states.

## 6. Management Modules

- [x] 6.1 Build group management list, create, rename, delete, and sort interactions.
- [x] 6.2 Build link list with group filter, search, pagination, copy, open, analytics, edit, and recycle actions.
- [x] 6.3 Build link create form with backend-supported validity behavior and no unsupported custom-domain control.
- [x] 6.4 Build link edit form with origin group, target group, URL, description, and validity fields.
- [x] 6.5 Build recycle bin list, restore, and permanent-remove interactions.

## 7. Analytics And Developer Modules

- [x] 7.1 Build analytics filters for group, optional link, date range, and quick ranges.
- [x] 7.2 Build PV, UV, UIP summary cards and trend charts.
- [x] 7.3 Build browser, OS, device, network, locale, visitor type, top IP, and access-record sections.
- [x] 7.4 Build API token list, create, plaintext-once display, status toggle, copy, and revoke interactions.
- [x] 7.5 Build profile settings view and update flow.

## 8. Verification

- [x] 8.1 Run frontend typecheck and production build.
- [x] 8.2 Run backend Maven package with frontend assets included.
- [x] 8.3 Run browser smoke tests for public create, login, dashboard route guard, group management, link management, recycle bin, analytics, token management, and profile settings.
- [x] 8.4 Verify `/app/**` refreshes serve the SPA and `/{shortUri}` remains handled by the redirect controller.

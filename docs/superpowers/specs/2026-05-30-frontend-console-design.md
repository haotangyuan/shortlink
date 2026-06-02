---
comet_change: add-frontend-console
role: technical-design
canonical_spec: openspec
---

# Frontend Console Design

## Summary

Add a root-level `frontend/` React application for the short-link service. The app uses React, TypeScript, Tailwind CSS, and Vite, and it is served by Spring Boot under `/app` in production so it does not conflict with root short-link redirects.

## Context

The backend already provides:

- Admin session APIs under `/api/short-link/admin/v1/**`.
- Core API-token APIs under `/api/short-link/v1/**`.
- Root short-link redirects through `GET /{shortUri}`.
- Statistics, recycle bin, groups, users, URL title lookup, and API token endpoints.

The reference product at `https://link.chanler.dev` confirms the desired product shape: public landing page, anonymous short-link creation, login/register, dashboard shell, group management, link management, recycle bin, analytics, profile, and developer token pages.

## Architecture

```text
Repository root
+-- frontend/                  React + TypeScript + Tailwind + Vite
|   +-- src/api/               Typed backend client
|   +-- src/app/               Route definitions and providers
|   +-- src/components/        Shared UI primitives and domain components
|   +-- src/features/          Page modules by domain
|   +-- dist/                  Vite production output
+-- src/main/java/...          Existing Spring Boot backend
+-- src/main/resources/static  Production SPA assets under /app
```

Runtime flow:

```text
Browser /app/** -----> Spring SPA forward -----> frontend index.html
Browser /api/** -----> Existing Spring controllers
Browser /{shortUri} -> Existing redirect controller
```

## Frontend Stack

- Vite React TypeScript template.
- Tailwind CSS v4 through `@tailwindcss/vite`.
- React Router with `basename="/app"`.
- TanStack Query for server state.
- React Hook Form + Zod for form validation.
- Recharts for analytics charts.
- Lucide React for icons.
- Local shadcn-style UI primitives instead of a large component framework.

## Backend Integration

The API client will unwrap the backend result envelope:

```ts
type ApiResult<T> = {
  code: string
  message?: string
  data?: T
  requestId?: string
}
```

The client treats `code === "0"` as success. HTTP 401 or token failure clears the admin session and redirects to `/app/login`.

Authenticated console requests use:

```text
Authorization: Bearer <session-token>
```

Core API examples in the developer center use API tokens against `/api/short-link/v1/**`, but the console itself calls admin endpoints.

## Page Modules

### Public Landing

Route: `/app`

- Hero and short-link creation card based on the reference site.
- Anonymous create request without Authorization.
- Result card with short link, original URL, copy button, and open button.
- Login/register links.

### Authentication

Routes: `/app/login`, `/app/register`

- Login uses username/password and stores the returned session token.
- Register uses username, password, real name, phone, and email.
- Protected routes validate stored session state before rendering.

### Dashboard

Route: `/app/dashboard`

- Aggregate cards: total links, groups, today PV, total PV.
- Quick actions for creating links, managing groups, and viewing analytics.
- Group overview list.

### Group Management

Route: `/app/dashboard/groups`

- Group cards with name, gid, sort order, and link count.
- Create, rename, delete, and sort actions.

### Link Management

Routes:

- `/app/dashboard/links`
- `/app/dashboard/links/create`
- `/app/dashboard/links/edit`

Features:

- Group filter and client-side search within loaded page.
- Paginated table showing short-link info, original URL, today/total PV, UV, UIP.
- Copy, open, edit, analytics, and move-to-recycle-bin actions.
- Create/edit forms hide custom-domain and permanent-validity controls because backend behavior does not support them as user-facing features today.

### Recycle Bin

Route: `/app/dashboard/recycle`

- Fetch recycled links for selected groups.
- Restore or permanently remove links after confirmation.

### Analytics

Route: `/app/dashboard/analytics`

- Group or single-link selector.
- Date range and quick ranges.
- PV/UV/UIP summary cards.
- Daily/hour/weekday trends.
- Browser, OS, device, network, locale, visitor type, top IP, and access record sections.

### Developer Tokens

Route: `/app/dashboard/developer/token`

- List API tokens with masked token, status, expiry, description, and update time.
- Create token and show plaintext token exactly once.
- Toggle enabled status.
- Revoke token.

Backend adjustment:

- Add `id` and `updateTime` to `TokenVO`.
- Populate both fields in `TokenServiceImpl.listTokens()`.

### Profile

Route: `/app/dashboard/profile`

- Show and update username-bound profile data.
- Refresh local user state after successful update.

## Routing Decision

Use `/app` instead of root-level frontend routes.

Reason: the backend already maps `GET /{shortUri}`. Root routes such as `/login` and `/dashboard` can be interpreted as short codes. `/app` avoids this conflict and still gives clean browser URLs.

## Build And Deployment

Development:

- Run the backend on `127.0.0.1:8068`.
- Run Vite from `frontend/`.
- Vite proxies `/api` to the backend.

Production:

- Build frontend assets.
- Copy Vite output into Spring static resources under an `/app` asset path.
- Spring serves `/app` and `/app/**` through the SPA fallback.
- Maven package includes the compiled assets.

## Verification

- Frontend typecheck.
- Frontend production build.
- Backend Maven package.
- Browser smoke tests for:
  - Public create.
  - Login/register.
  - Protected route redirect.
  - Group CRUD.
  - Link create/list/edit/recycle.
  - Analytics rendering.
  - Token create/toggle/revoke.
  - `/app/**` route refresh.
  - `/{shortUri}` redirect still handled by backend.

## Risks

- **Route collision**: addressed by `/app` prefix.
- **Admin OpenAPI incompleteness**: addressed by manually typed API modules.
- **Token UI missing ID**: addressed by backend `TokenVO` field addition.
- **Misleading create form**: addressed by hiding unsupported custom-domain and permanent-validity controls.
- **Dev CORS issues**: addressed by Vite proxy.

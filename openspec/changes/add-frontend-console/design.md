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

## Risks / Trade-offs

- Route collision risk with short codes -> Serve the frontend only under `/app` and leave root routes for redirects.
- Backend contract drift -> Keep all DTO/VO-shaped frontend types in `frontend/src/api/types.ts` and avoid ad hoc request bodies in components.
- Token management cannot work with the current list response -> Add `id` and `updateTime` to `TokenVO` and mapper conversion.
- CORS complexity during development -> Configure Vite dev-server proxy to `http://127.0.0.1:8068` so browser requests remain same-origin from the frontend perspective.
- Build complexity increases -> Add explicit npm scripts and document the two-step build path: frontend build first, then Maven package includes static assets.
- Anonymous public links are constrained by backend public-user behavior -> The public create form will call the anonymous admin create endpoint without `Authorization` and display backend errors directly.

## Migration Plan

1. Add the root `frontend/` project and build it independently with Vite.
2. Add Spring Boot SPA forwarding and static asset serving for `/app`.
3. Build the public page and authentication flow first.
4. Build authenticated modules against admin APIs.
5. Add TokenVO response fields before implementing token list actions.
6. Wire the production asset copy step into the project build.
7. Verify with frontend typecheck/build, backend Maven package, and browser smoke tests.

Rollback strategy: remove `/app` forwarding and compiled assets; existing backend API and short-link redirect behavior remain unchanged.

## Open Questions

- None for the approved first version. Future custom-domain and permanent-validity UI should wait until backend behavior supports those features.

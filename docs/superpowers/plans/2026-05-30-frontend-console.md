---
change: add-frontend-console
design-doc: docs/superpowers/specs/2026-05-30-frontend-console-design.md
base-ref: 18f64adb836998b9a1553bd59b4a30cdf53b7bfa
---

# Frontend Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a root-level React + TypeScript + Tailwind CSS frontend console for the short-link backend and serve it from Spring Boot under `/app`.

**Architecture:** The repository gains a root `frontend/` Vite app with typed API modules, React Router `basename="/app"`, and dashboard feature modules. Spring Boot keeps all existing API and redirect routes, adds `/app/**` SPA fallback, and serves compiled frontend assets without changing `GET /{shortUri}`.

**Tech Stack:** React, TypeScript, Vite, Tailwind CSS, React Router, TanStack Query, React Hook Form, Zod, Recharts, Lucide React, Spring Boot MVC static routing.

---

## File Structure

- Create `frontend/package.json`: frontend scripts and dependencies.
- Create `frontend/index.html`: Vite HTML entry.
- Create `frontend/vite.config.ts`: React plugin, Tailwind plugin, `/api` proxy, `/app/` base.
- Create `frontend/tsconfig.json`, `frontend/tsconfig.node.json`: TypeScript settings.
- Create `frontend/src/main.tsx`: React entry and providers.
- Create `frontend/src/app/router.tsx`: route tree with `/app` basename.
- Create `frontend/src/app/providers.tsx`: QueryClient, auth provider, toast provider.
- Create `frontend/src/api/types.ts`: DTO/VO-aligned frontend types.
- Create `frontend/src/api/client.ts`: result-envelope fetch client.
- Create `frontend/src/api/admin.ts`: admin API methods.
- Create `frontend/src/api/core.ts`: developer-facing core API snippets/helpers.
- Create `frontend/src/store/auth.tsx`: auth state, storage, login, logout, check-login.
- Create `frontend/src/components/ui/*.tsx`: local UI primitives used by pages.
- Create `frontend/src/components/layout/*.tsx`: public and dashboard layouts.
- Create `frontend/src/features/**`: page modules by domain.
- Modify `src/main/java/dev/haotangyuan/shortlink/controller/core/ScalarController.java` or add a dedicated MVC config/controller for `/app` fallback.
- Modify `src/main/java/dev/haotangyuan/shortlink/vo/TokenVO.java`: add `id` and `updateTime`.
- Modify `src/main/java/dev/haotangyuan/shortlink/service/impl/TokenServiceImpl.java`: populate token fields.
- Modify `README.md`: document frontend development and production build.

### Task 1: Frontend Project Skeleton

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/src/styles.css`
- Create: `frontend/src/main.tsx`

- [ ] **Step 1: Create package and scripts**

Use this dependency set in `frontend/package.json`:

```json
{
  "name": "shortlink-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite --host 127.0.0.1",
    "build": "tsc -b && vite build",
    "typecheck": "tsc -b --pretty false",
    "preview": "vite preview --host 127.0.0.1"
  },
  "dependencies": {
    "@hookform/resolvers": "latest",
    "@tailwindcss/vite": "latest",
    "@tanstack/react-query": "latest",
    "clsx": "latest",
    "lucide-react": "latest",
    "react": "latest",
    "react-dom": "latest",
    "react-hook-form": "latest",
    "react-router-dom": "latest",
    "recharts": "latest",
    "tailwind-merge": "latest",
    "tailwindcss": "latest",
    "zod": "latest"
  },
  "devDependencies": {
    "@types/react": "latest",
    "@types/react-dom": "latest",
    "@vitejs/plugin-react": "latest",
    "typescript": "latest",
    "vite": "latest"
  }
}
```

- [ ] **Step 2: Configure Vite**

Create `frontend/vite.config.ts`:

```ts
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  base: "/app/",
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8068",
        changeOrigin: true,
      },
      "/page": {
        target: "http://127.0.0.1:8068",
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: "dist",
    emptyOutDir: true,
  },
});
```

- [ ] **Step 3: Add TypeScript config**

Create `frontend/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "useDefineForClassFields": true,
    "lib": ["DOM", "DOM.Iterable", "ES2022"],
    "allowJs": false,
    "skipLibCheck": true,
    "esModuleInterop": true,
    "allowSyntheticDefaultImports": true,
    "strict": true,
    "forceConsistentCasingInFileNames": true,
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx"
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

Create `frontend/tsconfig.node.json`:

```json
{
  "compilerOptions": {
    "composite": true,
    "module": "ESNext",
    "moduleResolution": "Bundler",
    "allowSyntheticDefaultImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 4: Add app entry**

Create `frontend/index.html`:

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>ShortLink - 短链接管理平台</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

Create `frontend/src/main.tsx`:

```tsx
import React from "react";
import ReactDOM from "react-dom/client";
import { AppProviders } from "./app/providers";
import { AppRouter } from "./app/router";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AppProviders>
      <AppRouter />
    </AppProviders>
  </React.StrictMode>,
);
```

- [ ] **Step 5: Add base styles**

Create `frontend/src/styles.css` with Tailwind import, CSS variables, and global base styles. Use a neutral work-focused palette with clear chart variables:

```css
@import "tailwindcss";

:root {
  color-scheme: light;
  --background: 0 0% 100%;
  --foreground: 222 47% 11%;
  --muted: 210 40% 96%;
  --muted-foreground: 215 16% 47%;
  --border: 214 32% 91%;
  --primary: 221 83% 53%;
  --primary-foreground: 0 0% 100%;
  --destructive: 0 72% 51%;
  --chart-1: 221 83% 53%;
  --chart-2: 160 84% 39%;
  --chart-3: 38 92% 50%;
  --chart-4: 262 83% 58%;
  --chart-5: 0 72% 51%;
}

body {
  margin: 0;
  min-width: 320px;
  min-height: 100vh;
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}
```

- [ ] **Step 6: Verify skeleton**

Run:

```bash
cd frontend
npm install
npm run typecheck
npm run build
```

Expected: dependency install succeeds, TypeScript exits 0, Vite build creates `frontend/dist`.

### Task 2: Spring SPA Fallback And TokenVO Backend Patch

**Files:**
- Create: `src/main/java/dev/haotangyuan/shortlink/common/config/FrontendSpaConfiguration.java`
- Modify: `src/main/java/dev/haotangyuan/shortlink/vo/TokenVO.java`
- Modify: `src/main/java/dev/haotangyuan/shortlink/service/impl/TokenServiceImpl.java`

- [ ] **Step 1: Add SPA route forwarding**

Create `FrontendSpaConfiguration`:

```java
package dev.haotangyuan.shortlink.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontendSpaConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app").setViewName("forward:/app/index.html");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
        registry.addViewController("/app/{path:[^\\.]*}").setViewName("forward:/app/index.html");
        registry.addViewController("/app/**/{path:[^\\.]*}").setViewName("forward:/app/index.html");
    }
}
```

- [ ] **Step 2: Add token response fields**

Update `TokenVO` with fields:

```java
/**
 * id
 */
private Long id;

/**
 * 更新时间
 */
private Date updateTime;
```

- [ ] **Step 3: Populate token fields**

In `TokenServiceImpl.listTokens()`, add:

```java
.id(each.getId())
.updateTime(each.getUpdateTime())
```

inside the `TokenVO.builder()` chain.

- [ ] **Step 4: Run backend compile**

Run:

```bash
mvn -DskipTests package
```

Expected: Maven exits 0 and the project packages successfully.

### Task 3: API Client, Types, Auth, And Router

**Files:**
- Create: `frontend/src/api/types.ts`
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/api/admin.ts`
- Create: `frontend/src/api/core.ts`
- Create: `frontend/src/store/auth.tsx`
- Create: `frontend/src/app/providers.tsx`
- Create: `frontend/src/app/router.tsx`
- Create: `frontend/src/components/layout/ProtectedRoute.tsx`

- [ ] **Step 1: Define shared API types**

Create `frontend/src/api/types.ts` with backend-aligned names:

```ts
export type ApiResult<T> = {
  code: string;
  message?: string;
  data?: T;
  requestId?: string;
};

export type UserLoginVO = { token: string };
export type UserVO = { id: number; username: string; realName?: string; phone?: string; mail?: string };
export type UserRegisterReq = { username: string; password: string; realName?: string; phone?: string; mail?: string };
export type UserUpdateReq = UserRegisterReq;

export type GroupVO = { gid: string; name: string; sortOrder: number; linkCount: number };
export type GroupSortReq = { gid: string; sortOrder: number };

export type LinkCreateReq = {
  originUrl: string;
  gid: string;
  createdType?: number;
  validDateType?: number;
  validDate?: string;
  describe?: string;
};

export type LinkUpdateReq = {
  originUrl: string;
  fullShortUrl: string;
  originGid: string;
  gid: string;
  validDateType: number;
  validDate?: string;
  describe?: string;
};

export type LinkCreateVO = { gid: string; originUrl: string; fullShortUrl: string };
export type LinkPageVO = {
  id: number;
  domain: string;
  shortUri: string;
  fullShortUrl: string;
  originUrl: string;
  gid: string;
  validDateType: number;
  validDate?: string;
  createTime: string;
  describe?: string;
  favicon?: string;
  totalPv: number;
  totalUv: number;
  totalUip: number;
  todayPv: number;
  todayUv: number;
  todayUip: number;
  delTime?: number;
};

export type PageResult<T> = { records: T[]; total: number; size: number; current: number; pages: number };
export type TokenVO = { id: number; tokenMasked: string; name: string; enableStatus: number; validDate?: string; describe?: string; updateTime?: string };
export type TokenCreateReq = { name: string; describe?: string; validDate?: string | null };
```

- [ ] **Step 2: Implement fetch client**

Create `frontend/src/api/client.ts`:

```ts
import type { ApiResult } from "./types";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code?: string,
    public readonly status?: number,
  ) {
    super(message);
  }
}

let sessionToken: string | null = localStorage.getItem("shortlink.sessionToken");

export function setSessionToken(token: string | null) {
  sessionToken = token;
  if (token) localStorage.setItem("shortlink.sessionToken", token);
  else localStorage.removeItem("shortlink.sessionToken");
}

export async function request<T>(path: string, init: RequestInit = {}, auth = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) headers.set("Content-Type", "application/json");
  if (auth && sessionToken) headers.set("Authorization", `Bearer ${sessionToken}`);

  const response = await fetch(path, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiResult<T> | null;

  if (response.status === 401) {
    setSessionToken(null);
    throw new ApiError("用户身份验证失败", payload?.code, response.status);
  }
  if (!payload) throw new ApiError("网络请求失败", undefined, response.status);
  if (payload.code !== "0") throw new ApiError(payload.message || "请求失败", payload.code, response.status);
  return payload.data as T;
}
```

- [ ] **Step 3: Implement admin API module**

Create `frontend/src/api/admin.ts` with one function per backend endpoint. Include at least these signatures:

```ts
import { request } from "./client";
import type { GroupSortReq, GroupVO, LinkCreateReq, LinkCreateVO, LinkPageVO, PageResult, TokenCreateReq, TokenVO, UserLoginVO, UserRegisterReq, UserUpdateReq, UserVO } from "./types";

const admin = "/api/short-link/admin/v1";

export const adminApi = {
  login: (username: string, password: string) =>
    request<UserLoginVO>(`${admin}/user/login`, { method: "POST", body: JSON.stringify({ username, password }) }, false),
  register: (body: UserRegisterReq) =>
    request<void>(`${admin}/user`, { method: "POST", body: JSON.stringify(body) }, false),
  getUser: (username: string) => request<UserVO>(`${admin}/user/${encodeURIComponent(username)}`),
  updateUser: (body: UserUpdateReq) => request<void>(`${admin}/user`, { method: "PUT", body: JSON.stringify(body) }),
  checkLogin: (username: string, token: string) =>
    request<boolean>(`${admin}/user/check-login?username=${encodeURIComponent(username)}&token=${encodeURIComponent(token)}`, { method: "POST" }),
  logout: (username: string, token: string) =>
    request<void>(`${admin}/user/logout?username=${encodeURIComponent(username)}&token=${encodeURIComponent(token)}`, { method: "DELETE" }),
  createPublicLink: (originUrl: string, describe?: string) =>
    request<LinkCreateVO>(`${admin}/create`, { method: "POST", body: JSON.stringify({ originUrl, describe, gid: "public" }) }, false),
  createLink: (body: LinkCreateReq) => request<LinkCreateVO>(`${admin}/create`, { method: "POST", body: JSON.stringify(body) }),
  updateLink: (body: unknown) => request<void>(`${admin}/update`, { method: "POST", body: JSON.stringify(body) }),
  getLinks: (gid: string, current = 1, size = 10) =>
    request<PageResult<LinkPageVO>>(`${admin}/page?gid=${encodeURIComponent(gid)}&current=${current}&size=${size}`),
  getGroups: () => request<GroupVO[]>(`${admin}/group`),
  createGroup: (name: string) => request<void>(`${admin}/group`, { method: "POST", body: JSON.stringify({ name }) }),
  updateGroup: (gid: string, name: string) => request<void>(`${admin}/group`, { method: "PUT", body: JSON.stringify({ gid, name }) }),
  deleteGroup: (gid: string) => request<void>(`${admin}/group?gid=${encodeURIComponent(gid)}`, { method: "DELETE" }),
  sortGroups: (body: GroupSortReq[]) => request<void>(`${admin}/group/sort`, { method: "POST", body: JSON.stringify(body) }),
  getTokens: () => request<TokenVO[]>(`${admin}/token`),
  createToken: (body: TokenCreateReq) => request<string>(`${admin}/token`, { method: "POST", body: JSON.stringify(body) }),
  updateTokenStatus: (id: number, enable: boolean) => request<void>(`${admin}/token/${id}/status?enable=${enable}`, { method: "PATCH" }),
  deleteToken: (id: number) => request<void>(`${admin}/token/${id}`, { method: "DELETE" }),
};
```

- [ ] **Step 4: Add auth provider**

Create `frontend/src/store/auth.tsx` with a React context that exposes `user`, `token`, `isAuthenticated`, `login`, `logout`, `refreshUser`, and `checkAuth`. Store `shortlink.username` and `shortlink.sessionToken` in localStorage.

- [ ] **Step 5: Add app providers and router**

Create `frontend/src/app/providers.tsx` with `QueryClientProvider` and `AuthProvider`. Create `frontend/src/app/router.tsx` using `createBrowserRouter` with `basename: "/app"`.

- [ ] **Step 6: Verify types**

Run:

```bash
cd frontend
npm run typecheck
```

Expected: TypeScript exits 0.

### Task 4: Shared UI And Layouts

**Files:**
- Create: `frontend/src/lib/cn.ts`
- Create: `frontend/src/components/ui/Button.tsx`
- Create: `frontend/src/components/ui/Card.tsx`
- Create: `frontend/src/components/ui/Input.tsx`
- Create: `frontend/src/components/ui/Textarea.tsx`
- Create: `frontend/src/components/ui/Select.tsx`
- Create: `frontend/src/components/ui/Dialog.tsx`
- Create: `frontend/src/components/ui/Toast.tsx`
- Create: `frontend/src/components/layout/PublicLayout.tsx`
- Create: `frontend/src/components/layout/DashboardLayout.tsx`

- [ ] **Step 1: Add class merge helper**

Create:

```ts
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 2: Add UI primitives**

Create small components for button, card, input, textarea, select, dialog, and toast. Each component must accept `className`, forward native props, and keep styling in one place.

- [ ] **Step 3: Add public layout**

`PublicLayout` renders a centered content container and footer. Public pages use this instead of dashboard navigation.

- [ ] **Step 4: Add dashboard layout**

`DashboardLayout` renders a responsive sidebar with these navigation items: dashboard, groups, links, recycle bin, analytics, developer token, profile, logout. It must collapse on desktop and become drawer-style navigation on mobile.

- [ ] **Step 5: Verify responsive shell**

Run frontend dev server and inspect `/app` and `/app/dashboard` in desktop and mobile widths. Expected: no overlapping text or clipped controls.

### Task 5: Public, Login, And Register Pages

**Files:**
- Create: `frontend/src/features/public/PublicHomePage.tsx`
- Create: `frontend/src/features/auth/LoginPage.tsx`
- Create: `frontend/src/features/auth/RegisterPage.tsx`

- [ ] **Step 1: Build public home**

Use the reference structure: brand hero, primary create button, login button, compact feature cards, create section, and footer. The first screen must make `ShortLink` and the create action visible.

- [ ] **Step 2: Wire anonymous create form**

The public form submits:

```ts
await adminApi.createPublicLink(values.originUrl, values.describe || undefined);
```

Expected success state shows `fullShortUrl`, `originUrl`, copy, and open buttons.

- [ ] **Step 3: Build login page**

The login form validates non-empty username/password, calls `login(username, password)`, and navigates to `/dashboard` on success.

- [ ] **Step 4: Build register page**

The register form validates username/password and optional profile fields, calls `adminApi.register`, then sends the user to `/login`.

- [ ] **Step 5: Browser smoke check**

Run:

```bash
cd frontend
npm run dev
```

Open `/app`, `/app/login`, and `/app/register`. Expected: forms render and client-side validation works before API submission.

### Task 6: Dashboard, Groups, Links, And Recycle Bin

**Files:**
- Create: `frontend/src/features/dashboard/DashboardPage.tsx`
- Create: `frontend/src/features/groups/GroupsPage.tsx`
- Create: `frontend/src/features/links/LinksPage.tsx`
- Create: `frontend/src/features/links/LinkCreatePage.tsx`
- Create: `frontend/src/features/links/LinkEditPage.tsx`
- Create: `frontend/src/features/recycle/RecycleBinPage.tsx`

- [ ] **Step 1: Build dashboard overview**

Load groups with TanStack Query. For each group, request stats for current year to date and today as needed. Display total links, group count, today PV, and total PV.

- [ ] **Step 2: Build groups page**

Implement create, rename, delete, and sort calls against `adminApi`. After each mutation, invalidate `["groups"]`.

- [ ] **Step 3: Build links page**

Implement group select, search input, paginated table, and row actions:

```ts
adminApi.getLinks(selectedGid, page, 10);
adminApi.moveToRecycleBin(gid, fullShortUrl);
```

Add missing methods to `adminApi` if they were not added in Task 3.

- [ ] **Step 4: Build link create page**

Fields: `originUrl`, `gid`, `describe`, `validDate`. Hide custom domain and permanent-validity controls. Submit `validDateType: 1` and append `23:59:59` for date-only input.

- [ ] **Step 5: Build link edit page**

Load the selected link from the current group page by `fullShortUrl`. Submit `originGid`, `gid`, `originUrl`, `describe`, `validDateType`, and `validDate`.

- [ ] **Step 6: Build recycle bin page**

Load all user group IDs by default and call the recycle-bin page endpoint with repeated `gidList` query params. Implement restore and remove actions.

- [ ] **Step 7: Verify management modules**

Manual test with a local backend:

```text
1. Register or log in.
2. Create a group.
3. Create a link in that group.
4. Confirm it appears in the link list.
5. Move it to the recycle bin.
6. Restore it.
```

Expected: each step updates UI state after the backend mutation succeeds.

### Task 7: Analytics, Tokens, And Profile

**Files:**
- Create: `frontend/src/features/analytics/AnalyticsPage.tsx`
- Create: `frontend/src/features/analytics/StatsCards.tsx`
- Create: `frontend/src/features/analytics/TrendChart.tsx`
- Create: `frontend/src/features/analytics/DistributionCharts.tsx`
- Create: `frontend/src/features/analytics/AccessRecordsTable.tsx`
- Create: `frontend/src/features/developer/TokensPage.tsx`
- Create: `frontend/src/features/profile/ProfilePage.tsx`

- [ ] **Step 1: Add stats API methods**

Extend `adminApi` with:

```ts
getLinkStats(fullShortUrl: string, gid: string, startDate: string, endDate: string, enableStatus = 0)
getGroupStats(gid: string, startDate: string, endDate: string)
getLinkAccessRecords(fullShortUrl: string, gid: string, startDate: string, endDate: string, current: number, size: number)
getGroupAccessRecords(gid: string, startDate: string, endDate: string, current: number, size: number)
```

- [ ] **Step 2: Build analytics filter panel**

Fields: group, link or group-view, date range, quick range buttons for today, 7 days, 30 days, and 90 days.

- [ ] **Step 3: Build analytics chart sections**

Render PV/UV/UIP cards, daily/hour/weekday trends, browser list, OS pie chart, device/network progress lists, locale pie chart, visitor type pie chart, top IP list, and access records table.

- [ ] **Step 4: Build token page**

Use `TokenVO.id` for status toggles and deletion:

```ts
await adminApi.updateTokenStatus(token.id, checked);
await adminApi.deleteToken(token.id);
```

On create success, show the plaintext token in a modal and do not persist it outside session memory.

- [ ] **Step 5: Build profile page**

Load current user from auth state and `adminApi.getUser(username)`. Submit `adminApi.updateUser(values)` and refresh auth user data on success.

- [ ] **Step 6: Verify analytics and developer modules**

Expected: analytics pages render empty states when no stats exist, token create shows plaintext once, token list actions use numeric IDs, and profile update refreshes displayed user fields.

### Task 8: Build Integration, Documentation, And Verification

**Files:**
- Modify: `README.md`
- Optionally create: `scripts/build-frontend.sh`
- Modify: `.gitignore` if frontend build artifacts should be excluded from source control

- [ ] **Step 1: Decide asset copy path**

Use one production path:

```text
src/main/resources/static/app/index.html
src/main/resources/static/app/assets/*
```

If assets are generated, do not commit `frontend/dist`. Commit source files and any deliberate static copy strategy only.

- [ ] **Step 2: Add frontend build documentation**

Document:

```bash
cd frontend
npm install
npm run dev
npm run typecheck
npm run build
```

Document backend packaging after assets are copied:

```bash
mvn -DskipTests package
```

- [ ] **Step 3: Run frontend checks**

Run:

```bash
cd frontend
npm run typecheck
npm run build
```

Expected: both commands exit 0.

- [ ] **Step 4: Run backend package**

Run:

```bash
mvn -DskipTests package
```

Expected: Maven exits 0.

- [ ] **Step 5: Run browser smoke tests**

Use Playwright or the in-app browser to verify:

```text
/app renders public page
/app/login renders login page
/app/register renders register page
/app/dashboard redirects to login when unauthenticated
/app/dashboard renders after login
/app/dashboard/groups can create and rename a group
/app/dashboard/links can create and recycle a link
/app/dashboard/recycle can restore a link
/app/dashboard/analytics renders charts or empty states
/app/dashboard/developer/token can create and revoke a token
/app/dashboard/profile can update profile fields
/{shortUri} is still handled by backend redirect routing
```

Expected: all flows work without route collisions or visible layout overlap.

- [ ] **Step 6: Update OpenSpec task state**

After implementation tasks are complete, check off matching items in `openspec/changes/add-frontend-console/tasks.md` and run:

```bash
openspec status --change add-frontend-console
```

Expected: OpenSpec reports all artifacts complete and implementation checklist updated.

## Self-Review

Spec coverage:

- SPA routing isolation: Task 2 and Task 8.
- Root-level frontend project: Task 1.
- Public creation: Task 5.
- Auth flow: Task 3 and Task 5.
- Dashboard: Task 6.
- Groups, links, recycle bin: Task 6.
- Analytics: Task 7.
- Developer tokens: Task 2 and Task 7.
- Profile: Task 7.
- Error/loading states: Task 3 and per-page tasks.
- Responsive UI: Task 4 and Task 8.

Placeholder scan:

- No placeholder task text remains.
- All implementation tasks name exact files or exact modules.
- All verification steps include commands or concrete expected behavior.

Type consistency:

- DTO/VO names match Java request and response classes.
- Token management depends on `TokenVO.id`, added in Task 2 before token page work.
- Router basename and backend route prefix both use `/app`.

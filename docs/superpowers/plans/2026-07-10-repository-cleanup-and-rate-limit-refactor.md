# 仓库治理与限流精简 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 整理项目文档和废弃规格目录，在 README 中沉淀可验证的五点简历描述，并在不改变限流行为的前提下去除 `RateLimitFilter` 的重复路径分支。

**Architecture:** 文档通过 Git 重命名保持历史连续性，`docs/` 仅保留 `superpowers/` 产物。限流重构用一个路径到令牌数的私有映射替换四个重复分支，测试直接调用同包的过滤器方法并校验各类路径使用的限流器、令牌数、超时和放行/拒绝行为。

**Tech Stack:** Java 21、Spring Boot 3、JUnit 5、Mockito、Spring Test、React/TypeScript、Markdown、Git。

## Global Constraints

- 保持 REST API、数据库结构、认证方式、缓存键和 Redis Stream 协议不变。
- `frontend/src/lib/` 是前端运行依赖，绝不删除；仅移除仓库根目录的空 `lib/`。
- `docs/` 完成后只能包含 `docs/superpowers/`；项目说明与开发记录放入 `notebook/`。
- README 简历描述只能陈述代码可验证能力；性能指标必须附带压测条件和来源。
- 不创建 Git 分支、不暂存、不提交；这些操作需要用户额外明确授权。

---

### Task 1: 迁移文档并移除废弃目录

**Files:**
- Move: `doc/` → `notebook/`
- Move: `docs/project-analysis.md` → `notebook/project-analysis.md`
- Delete: `openspec/`
- Delete: `lib/`
- Modify: `CLAUDE.md:5`
- Modify: `README.md:106`
- Modify: `README.md:118`
- Modify: `README.md:122`
- Modify: `README.md:142`
- Modify: `notebook/rules/开发者准则.md:18`
- Modify: `notebook/intro/ShortLink项目技术文档.md:177`
- Modify: `notebook/project-analysis.md:71`

**Interfaces:**
- Consumes: 已有项目 Markdown 文档和 `openspec/` 历史规格。
- Produces: 仅含 `superpowers/` 的 `docs/`，以及所有项目笔记位于 `notebook/` 的统一文档入口。

- [ ] **Step 1: 重命名并删除目录**

Run:

```bash
git mv doc notebook
git mv docs/project-analysis.md notebook/project-analysis.md
git rm -r openspec
rmdir lib
rm -f docs/.DS_Store docs/superpowers/.DS_Store
```

Expected: `doc/`、`openspec/` 和根目录 `lib/` 不再存在；`notebook/` 包含原 `doc/` 内容及 `project-analysis.md`。

- [ ] **Step 2: 更新迁移后的链接和目录说明**

Use `apply_patch` for content edits:

```markdown
<!-- CLAUDE.md -->
**开始任何开发工作前，必须先阅读 [notebook/rules/开发者准则.md](notebook/rules/开发者准则.md)。**

<!-- notebook/rules/开发者准则.md -->
- **`notebook/dev/`**：编写本次开发记录（见第 3 条）

每次开发完成后，在 `notebook/dev/` 下新建文档，命名格式：`YYYY-MM-DD-变更内容.md`
```

Replace every project-document link in `README.md` from `doc/...` to `notebook/...`. In `notebook/intro/ShortLink项目技术文档.md`, change the frontend tree label from `lib/` to `frontend/src/lib/`. In `notebook/project-analysis.md`, remove the obsolete `openspec/` entry from the repository tree.

- [ ] **Step 3: 检查迁移结果**

Run:

```bash
test -d docs/superpowers
test ! -e doc
test ! -e openspec
test ! -e lib
git grep -n 'doc/' -- ':!docs/superpowers/**' ':!notebook/**' || true
git grep -n 'openspec' -- ':!docs/superpowers/**' ':!notebook/**' || true
```

Expected: 前四个目录断言通过；代码、README 和 CLAUDE 中没有残留的旧目录引用。

### Task 2: 固化 README 简历描述与维护规则

**Files:**
- Modify: `README.md:8`
- Modify: `notebook/rules/开发者准则.md:15`

**Interfaces:**
- Consumes: 当前短码、跳转、统计、限流、AI/MCP 的已实现能力。
- Produces: 可直接复用的五点简历描述，及后续变更同步规则。

- [ ] **Step 1: 在 README 功能列表后加入简历描述模块**

Use `apply_patch` to insert the following section after `## Features`:

```markdown
## 简历描述

LinkPilot 是一个基于 Spring Boot 3、Redis、MySQL、ShardingSphere、Caffeine 和 React 构建的智能短链管理平台，支持短链创建、跳转、统计分析和运营管理。

- **分布式短码生成：** 使用 Redis `INCRBY` 批量分配号段，本地原子递增和异步预取减少分配请求；通过仿射置换与定长 Base62 编码生成不可顺序猜测的 6/7 位短码。
- **高并发跳转链路：** 构建 Caffeine、Redis、短码快速否定、Redis Bloom Filter、空值缓存与数据库的多级查询链路；对回源使用按短链键的本地锁和双重检查，避免缓存击穿。
- **可靠异步统计：** 跳转数据写入 Redis Stream，由消费者组异步处理；以幂等状态、数据库消息唯一键和业务成功后 ACK 控制重复消费，并通过 `XAUTOCLAIM` 定时补偿 Pending 消息；使用 HyperLogLog 记录 UV/UIP。
- **分层流量保护：** 使用 Guava `RateLimiter` 对创建、跳转和统计接口实施进程内限流，并以 Redis + Lua 实现用户维度的滑动窗口风控。
- **AI 与开放集成：** 基于 AgentScope ReAct Agent 提供流量查询、分组统计、链接对比、异常检测和失效诊断等运营分析能力；通过 MCP SSE 端点向外部智能体提供短链创建工具。

> 维护规则：仅描述仓库中已落地且可验证的能力；引入、删除或实质改变上述能力时，同步更新本节。性能数据必须注明压测条件和结果来源。
```

- [ ] **Step 2: 将同步要求写入开发者准则**

Use `apply_patch` to add this line to the existing README documentation rule in `notebook/rules/开发者准则.md`:

```markdown
- **`README.md` 的“简历描述”**：短码、跳转、统计、限流或 AI/MCP 能力发生实质变化时，更新对应要点；不得补写未经压测验证的性能数据。
```

- [ ] **Step 3: 检查措辞与链接**

Run:

```bash
git diff --check
rg -n '## 简历描述|维护规则|notebook/' README.md notebook/rules/开发者准则.md CLAUDE.md
```

Expected: 无空白错误；README 包含完整五点描述，迁移后的规则与链接均使用 `notebook/`。

### Task 3: 用测试锁定限流行为并合并重复分支

**Files:**
- Create: `src/test/java/dev/haotangyuan/shortlink/common/web/RateLimitFilterTest.java`
- Modify: `src/main/java/dev/haotangyuan/shortlink/common/web/RateLimitFilter.java:34`

**Interfaces:**
- Consumes: `RateLimitFilter(RateLimiter createRateLimiter, RateLimiter redirectRateLimiter, RateLimiter statsRateLimiter, RateLimitProperties props)`。
- Produces: `RateLimitFilter` 对四个创建端点使用同一条令牌数映射逻辑，且保留 `tryAcquire(int permits, long timeout, TimeUnit.MILLISECONDS)` 调用语义。

- [ ] **Step 1: 创建行为锁定测试**

Create `RateLimitFilterTest` in the same package as the filter. Use `MockHttpServletRequest`、`MockHttpServletResponse`、Mockito mock 的 `RateLimiter` 和 `FilterChain`。测试应设置三个超时值：创建 17ms、跳转 19ms、统计 23ms，并覆盖：

```java
@Test
void appliesExistingPermitCountsToAllCreationPaths() throws Exception {
    when(createRateLimiter.tryAcquire(anyInt(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

    for (String path : List.of(
            "/api/short-link/v1/create",
            "/api/short-link/admin/v1/create",
            "/api/short-link/v1/create/batch",
            "/api/short-link/admin/v1/create/batch")) {
        filter.doFilterInternal(postRequest(path), new MockHttpServletResponse(), filterChain);
    }

    verify(createRateLimiter, times(2)).tryAcquire(1, 17, TimeUnit.MILLISECONDS);
    verify(createRateLimiter, times(2)).tryAcquire(5, 17, TimeUnit.MILLISECONDS);
    verify(filterChain, times(4)).doFilter(any(), any());
}
```

Add one test that confirms `GET /abc123` calls only `redirectRateLimiter.tryAcquire(1, 19, MILLISECONDS)`, and `GET /api/short-link/admin/v1/stats` calls only `statsRateLimiter.tryAcquire(1, 23, MILLISECONDS)`. Add one rejection test in which the creation limiter returns `false`, asserting HTTP 429 and `verifyNoInteractions(filterChain)`.

- [ ] **Step 2: 运行行为锁定测试**

Run:

```bash
mvn -Dtest=RateLimitFilterTest test
```

Expected: PASS. This is a behavior-preserving refactor, so the test establishes the existing observable contract before the implementation is simplified.

- [ ] **Step 3: 合并创建路径分支**

Use `apply_patch` to add one private mapping method and replace the four duplicated branches:

```java
private int creationPermitCount(String path) {
    return switch (path) {
        case "/api/short-link/v1/create", "/api/short-link/admin/v1/create" -> 1;
        case "/api/short-link/v1/create/batch", "/api/short-link/admin/v1/create/batch" -> 5;
        default -> 0;
    };
}
```

In `doFilterInternal`, after the redirect branch and before the statistics branch, call the mapping once:

```java
int permitCount = creationPermitCount(path);
if (permitCount > 0
        && !createRateLimiter.tryAcquire(permitCount, props.getCreate().getTimeout(), TimeUnit.MILLISECONDS)) {
    tooMany(req, resp);
    return;
}
```

Keep the statistics branch as `else if` when `permitCount == 0`; do not alter `isRedirectPath`, `tooMany`, qualifier names, filter order, time units or response payloads.

- [ ] **Step 4: 重新运行限流测试**

Run:

```bash
mvn -Dtest=RateLimitFilterTest test
```

Expected: PASS, with the same four creation-path permit assertions and unchanged redirect/statistics behavior.

### Task 4: 执行回归验证并记录开发变更

**Files:**
- Create: `notebook/dev/2026-07-10-仓库治理与限流精简.md`
- Verify: `README.md`
- Verify: `docs/superpowers/`
- Verify: `src/main/java/dev/haotangyuan/shortlink/common/web/RateLimitFilter.java`
- Verify: `src/test/java/dev/haotangyuan/shortlink/common/web/RateLimitFilterTest.java`

**Interfaces:**
- Consumes: Tasks 1–3 的已完成变更及测试结果。
- Produces: 通过后端、前端、目录和静态检查验证的可交付工作区，以及与实际验证结果一致的开发记录。

- [ ] **Step 1: 运行完整验证**

Run:

```bash
mvn test
(cd frontend && npm test)
(cd frontend && npm run build)
git diff --check
test -d docs/superpowers
test ! -e doc
test ! -e openspec
test ! -e lib
test -z "$(find docs -mindepth 1 -maxdepth 1 ! -name superpowers -print)"
```

Expected: 所有命令以状态码 0 结束；后端与前端测试、前端构建均通过；`docs/` 仅含 `superpowers/`。

- [ ] **Step 2: 搜索失效引用**

Run:

```bash
git grep -n 'doc/' -- ':!docs/superpowers/**' ':!notebook/**' || true
git grep -n 'openspec' -- ':!docs/superpowers/**' ':!notebook/**' || true
git grep -n 'lib/' -- ':!frontend/src/lib/**' ':!notebook/intro/ShortLink项目技术文档.md' || true
```

Expected: 没有指向旧项目文档目录、OpenSpec 或根目录 `lib/` 的有效引用。

- [ ] **Step 3: 写入开发记录**

After validation, create `notebook/dev/2026-07-10-仓库治理与限流精简.md` with five factual sections: 背景、方案、影响范围、测试、遗留问题。记录目录迁移、OpenSpec 删除、README 简历模块和限流分支合并；测试部分只写入本任务实际运行的命令及结果；遗留问题明确标记 `LinkServiceImpl#pageLink` 当日 PV 的 N+1 查询为后续独立优化。

- [ ] **Step 4: 最终检查变更范围**

Run:

```bash
git status --short
git diff --stat
```

Expected: 只包含本计划声明的文档移动、OpenSpec 删除、README/规则更新、限流过滤器、其测试与开发记录；不包含分支、暂存或提交操作。

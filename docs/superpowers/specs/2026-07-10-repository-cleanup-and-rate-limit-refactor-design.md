# 仓库治理与限流精简设计

**目标：** 在不改变公开 API、数据库结构和短链业务行为的前提下，完成文档目录治理、README 简历描述沉淀，并消除限流过滤器中的重复分支。

## 范围

1. 将根目录 `doc/` 迁移为 `notebook/`，将 `docs/project-analysis.md` 迁入 `notebook/project-analysis.md`。
2. 保留 `docs/superpowers/`，使 `docs/` 仅用于 Superpowers 的设计、计划和验证记录。
3. 删除根目录空 `lib/` 和完整 `openspec/` 目录；不触碰 `frontend/src/lib/`，其中包含前端运行时使用的日期与 className 工具。
4. 更新 `README.md`、`CLAUDE.md` 及受迁移影响的 Markdown 链接和目录树。
5. 在 `README.md` 新增可直接用于简历的项目描述区，并规定功能或架构变化时同步更新该区内容。
6. 合并 `RateLimitFilter` 中前台和后台创建、批量创建接口的重复限流分支，保持原有路径、令牌消耗数量、超时配置和 429 响应不变。

## 非目标

- 不修改 REST API、认证方式、短链接有效期规则、缓存键、Redis Stream 协议或数据库表结构。
- 不删除或重命名前端的 `frontend/src/lib/`。
- 不在本轮拆分 `LinkServiceImpl`，也不修改它的跳转缓存流程。
- 不声称未经压测验证的 QPS、延迟、内存节省或可用性指标。

## 文档迁移

| 当前位置 | 目标位置 | 处理方式 |
| --- | --- | --- |
| `doc/` | `notebook/` | 整目录重命名，保留全部项目文档与开发准则。 |
| `docs/project-analysis.md` | `notebook/project-analysis.md` | 迁入项目笔记目录，并删除其中已失效的 OpenSpec 目录说明。 |
| `docs/superpowers/` | `docs/superpowers/` | 原样保留。 |
| `openspec/` | 删除 | 删除已不再使用的配置、变更提案与规格。 |
| 根目录 `lib/` | 删除 | 目录为空且未受 Git 跟踪。 |

`README.md` 的项目文档链接改为 `notebook/`；`CLAUDE.md` 的开发前置阅读路径改为 `notebook/rules/开发者准则.md`；开发者准则中的开发记录路径同步改为 `notebook/dev/`。

## README 简历描述

新增 `## 简历描述` 区，采用以下五个可验证能力点：

1. **分布式短码生成：** 使用 Redis `INCRBY` 批量分配号段，本地原子递增和异步预取减少分配请求；通过仿射置换与定长 Base62 编码生成不可顺序猜测的 6/7 位短码。
2. **高并发跳转链路：** 构建 Caffeine、Redis、短码快速否定、Redis Bloom Filter、空值缓存与数据库的多级查询链路；对回源使用按短链键的本地锁和双重检查，避免缓存击穿。
3. **可靠异步统计：** 跳转数据写入 Redis Stream，由消费者组异步处理；以幂等状态、数据库消息唯一键和业务成功后 ACK 控制重复消费，并通过 `XAUTOCLAIM` 定时补偿 Pending 消息；使用 HyperLogLog 记录 UV/UIP。
4. **分层流量保护：** 使用 Guava `RateLimiter` 对创建、跳转和统计接口实施进程内限流，并以 Redis + Lua 实现用户维度的滑动窗口风控。
5. **AI 与开放集成：** 基于 AgentScope ReAct Agent 提供流量查询、分组统计、链接对比、异常检测和失效诊断等运营分析能力；通过 MCP SSE 端点向外部智能体提供短链创建工具。

该区补充维护规则：只描述仓库中已落地且可验证的能力；引入、删除或实质改变上述能力时，必须同步更新五个要点；性能数据必须注明压测条件和结果来源。

## 限流过滤器精简

当前 `RateLimitFilter` 对以下路径分别编写了相同的限流调用：

- `/api/short-link/v1/create`
- `/api/short-link/admin/v1/create`
- `/api/short-link/v1/create/batch`
- `/api/short-link/admin/v1/create/batch`

改造后由私有路径匹配方法归类为“单条创建”或“批量创建”，再统一调用 `createRateLimiter`。单条创建继续消耗 1 个令牌，批量创建继续消耗 5 个令牌；跳转与统计分支、限流超时、响应体和过滤器顺序均保持不变。

## 验证

1. 运行新增的 `RateLimitFilter` 单元测试，覆盖前台/后台单条创建、前台/后台批量创建、统计和跳转路径的令牌消耗及放行行为。
2. 运行后端 Maven 测试与前端测试，确认迁移不影响构建。
3. 使用 `git diff --check` 和路径搜索，确认无残留 `doc/`、根目录 `lib/` 或 `openspec/` 引用，且 `docs/` 仅保留 `superpowers/`。

## 已识别的后续优化

`LinkServiceImpl#pageLink` 已批量读取 Redis 中的 UV/UIP，但仍会为分页中的每条短链单独查询当日 PV，存在 N+1 查询。该项涉及 Mapper 聚合查询与数据访问验证，应作为独立变更处理，避免与本次低风险重构混合。

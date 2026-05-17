local username = KEYS[1]
local timeWindowSec = tonumber(ARGV[1]) -- 时间窗口，单位：秒

-- 构造 Redis 中存储用户访问记录的 ZSET 键名
local accessKey = "short-link:user-flow-risk-control:" .. username

-- 当前时间戳（毫秒）
local now = redis.call("TIME")
local nowMillis = now[1] * 1000 + math.floor(now[2] / 1000)
local windowStart = nowMillis - timeWindowSec * 1000

-- 滑动窗口：移除窗口外数据
redis.call("ZREMRANGEBYSCORE", accessKey, 0, windowStart)

-- 记录本次访问（member 追加随机数避免同毫秒重复）
local member = tostring(nowMillis) .. ":" .. tostring(math.random(0, 9999))
redis.call("ZADD", accessKey, nowMillis, member)

-- 获取窗口内请求数
local currentAccessCount = redis.call("ZCARD", accessKey)

-- 设置过期时间，避免长时间空闲占用空间
redis.call("PEXPIRE", accessKey, timeWindowSec * 1000)

-- 返回当前访问次数
return currentAccessCount

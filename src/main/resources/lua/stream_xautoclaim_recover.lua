-- 使用 XAUTOCLAIM 恢复超时 Pending（非 JUSTID，直接返回字段值）
-- KEYS[1] = stream key
-- ARGV[1] = group
-- ARGV[2] = consumer
-- ARGV[3] = minIdleMs
-- ARGV[4] = startId (e.g. '0-0')
-- ARGV[5] = count
local key = KEYS[1]
local group = ARGV[1]
local consumer = ARGV[2]
local minIdle = tonumber(ARGV[3]) or 120000
local startId = ARGV[4] or '0-0'
local count = tonumber(ARGV[5]) or 100
-- 返回格式（Redis 原生）: 
-- { nextStartId, { {id1, {f1, v1, f2, v2, ...}}, {id2, {f1, v1, ...}}, ... }, deletedIds }
return redis.call('XAUTOCLAIM', key, group, consumer, minIdle, startId, 'COUNT', count)

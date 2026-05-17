-- hll_count_add_delta.lua
-- KEYS[1] = HLL key  (uv/uip)
-- KEYS[2] = active set key (uv/uip)
-- ARGV[1] = member (uv/ip)
-- ARGV[2] = fullShortUrl (for active set)
-- ARGV[3] = ttlSeconds (int, e.g. 259200)
local before = redis.call('PFCOUNT', KEYS[1])
redis.call('PFADD', KEYS[1], ARGV[1])
-- 将 fsu 记入活跃集合（幂等）
redis.call('SADD', KEYS[2], ARGV[2])
-- TTL 兜底：首次出现或无 TTL 时设置统一过期
local ttl1 = redis.call('TTL', KEYS[1])
if not ttl1 or ttl1 < 0 then redis.call('EXPIRE', KEYS[1], ARGV[3]) end
local ttl2 = redis.call('TTL', KEYS[2])
if not ttl2 or ttl2 < 0 then redis.call('EXPIRE', KEYS[2], ARGV[3]) end
local after = redis.call('PFCOUNT', KEYS[1])
local delta = after - before
if delta < 0 then delta = 0 end
return delta
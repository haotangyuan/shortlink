-- hll_pfcount_batch.lua
-- 批量计算多个 HLL 键的 PFCOUNT
-- KEYS[1] = key_prefix (例如 "short-link:stats:uv:0:")
-- ARGV[1] = 多个 fullShortUrl，以逗号分隔
-- 返回一个数组，每个元素对应一个 FSU 的 PFCOUNT 结果

local prefix = KEYS[1]
local fsuList = ARGV[1]

-- 解析 FSU 列表
local fsus = {}
for fsu in string.gmatch(fsuList, "([^,]+)") do
    table.insert(fsus, fsu)
end

if #fsus == 0 then
    return {}
end

-- 批量计算每个键的 PFCOUNT
local results = {}
for i, fsu in ipairs(fsus) do
    local key = prefix .. fsu
    local count = redis.call('PFCOUNT', key)
    table.insert(results, count)
end

return results
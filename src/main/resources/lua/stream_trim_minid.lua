-- 清理 Stream 已消费的老消息（安全：不删除未消费/未ACK的消息）
-- KEYS[1] = stream key
-- ARGV[1] = group name
-- ARGV[2] = buffer size (number)
local key = KEYS[1]
local group = ARGV[1]
local buffer = tonumber(ARGV[2]) or 1000

-- 1) 若有 Pending，取其最老的 id 作为保留下限
local pend = redis.call('XPENDING', key, group, '-', '+', 1)
if type(pend) == 'table' and #pend > 0 then
  local entry = pend[1]
  if type(entry) == 'table' and #entry >= 1 then
    local pid = tostring(entry[1])
    return redis.call('XTRIM', key, 'MINID', pid)
  end
end

-- 2) 无 Pending：找到该 group 的 last-delivered-id
local groups = redis.call('XINFO', 'GROUPS', key)
local last_id = nil
for i = 1, #groups do
  local g = groups[i]
  if type(g) == 'table' then
    local name_val = nil
    for j = 1, #g, 2 do
      local k = tostring(g[j])
      local v = g[j+1]
      if k == 'name' then name_val = tostring(v) end
      if k == 'last-delivered-id' and name_val == group then
        last_id = tostring(v)
        break
      end
    end
  end
end
if (not last_id) or last_id == '0-0' then
  return 0
end

-- 3) 以 last-delivered-id 为上界，向前取 buffer 条，取最老的一条作为边界
local msgs = redis.call('XREVRANGE', key, last_id, '-', 'COUNT', buffer)
if type(msgs) == 'table' and #msgs > 0 then
  local last = msgs[#msgs]
  if type(last) == 'table' and #last >= 1 then
    local boundary = tostring(last[1])
    return redis.call('XTRIM', key, 'MINID', boundary)
  end
end
return 0

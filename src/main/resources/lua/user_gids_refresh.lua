-- Rebuild user_gids set and set TTL
-- KEYS[1]: set key (short-link:user-gids:{username})
-- ARGV[1]: ttl seconds (number)
-- ARGV[2..n]: gid values

local key = KEYS[1]
local ttl = tonumber(ARGV[1])

-- clear existing set
redis.call('DEL', key)

-- add all gid values
for i = 2, #ARGV do
  redis.call('SADD', key, ARGV[i])
end

-- set ttl if valid
if ttl and ttl > 0 then
  redis.call('EXPIRE', key, ttl)
end

return #ARGV - 1


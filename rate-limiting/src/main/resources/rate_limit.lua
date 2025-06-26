local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_start = tonumber(ARGV[2])
local current_time = tonumber(ARGV[3])
local window_size_secs = tonumber(ARGV[4])

-- Remove expired entries
redis.call('ZREMRANGEBYSCORE', key, 0, window_start)

-- Count current requests
local current_count = redis.call('ZCARD', key)

if current_count >= limit then
  return 1
end

-- Add current request
redis.call('ZADD', key, current_time, current_time)

-- Set expiration
redis.call('EXPIRE', key, window_size_secs)

return 0
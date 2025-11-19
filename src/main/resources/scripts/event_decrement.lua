-- KEYS[1] : event:{id}
-- ARGV[1] : perPrice
-- ARGV[2] : statusId (COMPLETED)

local status = redis.call('HGET', KEYS[1], 'status')
if status == 'COMPLETED' then
    return -99999  -- 이미 종료된 이벤트
end

local target = redis.call('HINCRBY', KEYS[1], 'target', -ARGV[1])
if target <= 0 then
    redis.call('HSET', KEYS[1], 'statusId', ARGV[2])
    redis.call('HSET', KEYS[1], 'status', 'COMPLETED')
end
return target
local v = redis.call('get', _:cau-key)

if v == false then
  return redis.error_reply("key does not exist")
end

if v ~= _:cau-old-val then
  return 0
end

if _:cau-ttl-ms ~= "0" then
  redis.call('psetex', _:cau-key, _:cau-ttl-ms, _:cau-new-val)
else
  redis.call('set', _:cau-key, _:cau-new-val)
end

return 1

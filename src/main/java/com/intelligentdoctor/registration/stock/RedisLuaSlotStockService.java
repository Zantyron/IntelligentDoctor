package com.intelligentdoctor.registration.stock;

import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import com.intelligentdoctor.registration.dto.ReservationToken;
import com.intelligentdoctor.tenant.TenantContext;
import com.intelligentdoctor.tenant.TenantRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "app.registration", name = "stock-provider", havingValue = "redis")
public class RedisLuaSlotStockService implements SlotStockService {

    private final StringRedisTemplate redisTemplate;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final TenantRegistry tenantRegistry;
    private final DefaultRedisScript<Long> reserveScript;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisLuaSlotStockService(StringRedisTemplate redisTemplate,
                                    ScheduleSlotRepository scheduleSlotRepository,
                                    TenantRegistry tenantRegistry) {
        this.redisTemplate = redisTemplate;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.tenantRegistry = tenantRegistry;
        this.reserveScript = new DefaultRedisScript<>();
        this.reserveScript.setResultType(Long.class);
        this.reserveScript.setScriptText("""
                local current = tonumber(redis.call('GET', KEYS[1]) or '-1')
                local quantity = tonumber(ARGV[1])
                if current < 0 then
                    return -2
                end
                if current < quantity then
                    return -1
                end
                redis.call('DECRBY', KEYS[1], quantity)
                return current - quantity
                """);
        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setResultType(Long.class);
        this.releaseScript.setScriptText("""
                local reservation = redis.call('GET', KEYS[2])
                if not reservation then
                    return 0
                end
                if reservation ~= ARGV[1] then
                    return -1
                end
                redis.call('INCRBY', KEYS[1], tonumber(ARGV[2]))
                redis.call('DEL', KEYS[2])
                return 1
                """);
    }

    @Override
    public ReservationToken reserve(String slotId, int quantity) {
        String hospitalId = TenantContext.requireHospitalId();
        String key = stockKey(hospitalId, slotId);
        ensureWarm(key, slotId);
        Long result = redisTemplate.execute(reserveScript, List.of(key), String.valueOf(quantity));
        if (result == null || result < 0) {
            String message = result != null && result == -2 ? "stock is not warmed" : "schedule slot stock is insufficient";
            return new ReservationToken(false, null, message);
        }
        String token = "redis-" + UUID.randomUUID();
        redisTemplate.opsForValue().set(reservationKey(hospitalId, token), reservationValue(slotId, quantity), 30, TimeUnit.MINUTES);
        return new ReservationToken(true, token, "Redis Lua reserved");
    }

    @Override
    public void release(String slotId, String token, int quantity) {
        String hospitalId = TenantContext.requireHospitalId();
        redisTemplate.execute(releaseScript,
                List.of(stockKey(hospitalId, slotId), reservationKey(hospitalId, token)),
                reservationValue(slotId, quantity),
                String.valueOf(quantity));
    }

    @Override
    public String providerName() {
        return "redis-lua";
    }

    private void ensureWarm(String key, String slotId) {
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return;
        }
        scheduleSlotRepository.findById(slotId).map(ScheduleSlotEntity::getStockAvailable)
                .ifPresent(stock -> redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(stock), 2, TimeUnit.DAYS));
    }

    private String stockKey(String hospitalId, String slotId) {
        return tenantRegistry.redisPrefix(hospitalId) + ":slot-stock:" + slotId;
    }

    private String reservationKey(String hospitalId, String token) {
        return tenantRegistry.redisPrefix(hospitalId) + ":reservation:" + token;
    }

    private String reservationValue(String slotId, int quantity) {
        return slotId + ":" + quantity;
    }
}

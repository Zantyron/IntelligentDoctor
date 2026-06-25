package com.intelligentdoctor.registration.stock;

import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import com.intelligentdoctor.registration.dto.ReservationToken;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.registration", name = "stock-provider", havingValue = "local")
public class LocalSlotStockService implements SlotStockService {

    private final ScheduleSlotRepository scheduleSlotRepository;

    public LocalSlotStockService(ScheduleSlotRepository scheduleSlotRepository) {
        this.scheduleSlotRepository = scheduleSlotRepository;
    }

    @Override
    @Transactional
    public ReservationToken reserve(String slotId, int quantity) {
        ScheduleSlotEntity slot = scheduleSlotRepository.findWithLockById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("排班不存在"));
        if (slot.getStockAvailable() < quantity) {
            return new ReservationToken(false, null, "号源已不足");
        }
        slot.setStockAvailable(slot.getStockAvailable() - quantity);
        scheduleSlotRepository.save(slot);
        return new ReservationToken(true, "local-" + UUID.randomUUID(), "预占成功");
    }

    @Override
    @Transactional
    public void release(String slotId, String token, int quantity) {
        scheduleSlotRepository.findWithLockById(slotId).ifPresent(slot -> {
            slot.setStockAvailable(slot.getStockAvailable() + quantity);
            scheduleSlotRepository.save(slot);
        });
    }

    @Override
    public String providerName() {
        return "local";
    }
}

package com.intelligentdoctor.registration.stock;

import com.intelligentdoctor.registration.dto.ReservationToken;

public interface SlotStockService {

    ReservationToken reserve(String slotId, int quantity);

    void release(String slotId, String token, int quantity);

    String providerName();
}

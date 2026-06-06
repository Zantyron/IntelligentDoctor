package com.intelligentdoctor.registration;

import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import com.intelligentdoctor.registration.dto.ConfirmRegistrationRequest;
import com.intelligentdoctor.registration.dto.CreateDraftCommand;
import com.intelligentdoctor.registration.repository.RegistrationOrderRepository;
import com.intelligentdoctor.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RegistrationConsistencyTests {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private RegistrationOrderRepository orderRepository;

    @Test
    void confirmShouldBeIdempotentAndDeductStockOnlyOnce() {
        ScheduleSlotEntity slot = scheduleSlotRepository.findAll().stream()
                .filter(candidate -> candidate.getStockAvailable() > 0)
                .findFirst()
                .orElseThrow();
        int before = slot.getStockAvailable();

        var draft = registrationService.createDraft(new CreateDraftCommand(
                slot.getHospitalId(),
                "consistency-session",
                "胸闷心慌，想预约专家号",
                slot.getDepartmentId(),
                slot.getClinicRoomId(),
                slot.getDoctorId(),
                slot.getId(),
                slot.getSlotDate(),
                slot.getPeriod(),
                null,
                null,
                null,
                null
        ));
        var request = new ConfirmRegistrationRequest(
                draft.draftId(),
                "consistency-session",
                "same-confirm-request",
                "张三",
                "13800000000",
                "310101199001011234"
        );

        var first = registrationService.confirm(request);
        var second = registrationService.confirm(request);
        ScheduleSlotEntity after = scheduleSlotRepository.findById(slot.getId()).orElseThrow();

        assertThat(second.orderNo()).isEqualTo(first.orderNo());
        assertThat(orderRepository.findByDraftId(draft.draftId())).isPresent();
        assertThat(after.getStockAvailable()).isEqualTo(before - 1);
    }
}

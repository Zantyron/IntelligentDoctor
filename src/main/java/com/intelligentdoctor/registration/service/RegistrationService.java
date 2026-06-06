package com.intelligentdoctor.registration.service;

import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.registration.dto.ConfirmRegistrationRequest;
import com.intelligentdoctor.registration.dto.CreateDraftCommand;
import com.intelligentdoctor.registration.dto.RegistrationDraftView;
import com.intelligentdoctor.registration.dto.RegistrationOrderView;
import com.intelligentdoctor.registration.dto.ReservationToken;
import com.intelligentdoctor.registration.entity.RegistrationDraftEntity;
import com.intelligentdoctor.registration.entity.RegistrationOrderEntity;
import com.intelligentdoctor.registration.event.RegistrationEventPublisher;
import com.intelligentdoctor.registration.event.RegistrationReservedEvent;
import com.intelligentdoctor.registration.repository.RegistrationDraftRepository;
import com.intelligentdoctor.registration.repository.RegistrationOrderRepository;
import com.intelligentdoctor.registration.stock.SlotStockService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RegistrationService {

    private final RegistrationDraftRepository draftRepository;
    private final RegistrationOrderRepository orderRepository;
    private final SlotStockService slotStockService;
    private final RegistrationEventPublisher eventPublisher;
    private final ChatHistoryService chatHistoryService;

    public RegistrationService(RegistrationDraftRepository draftRepository,
                               RegistrationOrderRepository orderRepository,
                               SlotStockService slotStockService,
                               RegistrationEventPublisher eventPublisher,
                               ChatHistoryService chatHistoryService) {
        this.draftRepository = draftRepository;
        this.orderRepository = orderRepository;
        this.slotStockService = slotStockService;
        this.eventPublisher = eventPublisher;
        this.chatHistoryService = chatHistoryService;
    }

    @Transactional
    public RegistrationDraftView createDraft(CreateDraftCommand command) {
        RegistrationDraftEntity entity = new RegistrationDraftEntity();
        entity.setHospitalId(command.hospitalId());
        entity.setSessionId(command.sessionId());
        entity.setSymptomSummary(command.symptomSummary());
        entity.setDepartmentId(command.departmentId());
        entity.setClinicRoomId(command.clinicRoomId());
        entity.setDoctorId(command.doctorId());
        entity.setSlotId(command.slotId());
        entity.setVisitDate(command.visitDate());
        entity.setVisitPeriod(command.visitPeriod());
        entity.setPatientName(command.patientName());
        entity.setPatientPhone(command.patientPhone());
        entity.setIdCard(command.idCard());
        entity.setPatientId(command.patientId());
        entity.setStatus("DRAFT");
        RegistrationDraftEntity saved = draftRepository.save(entity);
        return toView(saved);
    }

    @Transactional
    public RegistrationOrderView confirm(ConfirmRegistrationRequest request) {
        RegistrationDraftEntity draft = draftRepository.findById(request.draftId())
                .orElseThrow(() -> new EntityNotFoundException("registration draft not found"));

        RegistrationOrderEntity existingOrder = orderRepository.findByDraftId(draft.getId()).orElse(null);
        if (existingOrder != null) {
            chatHistoryService.storeToolTrace(request.sessionId(), "confirmRegistration",
                    Map.of("draftId", request.draftId(), "idempotencyKey", idempotencyKey(request)),
                    Map.of("orderNo", existingOrder.getOrderNo(), "idempotent", true));
            return toView(existingOrder);
        }

        validateDraft(draft);
        draft.setPatientName(request.patientName());
        draft.setPatientPhone(request.patientPhone());
        draft.setIdCard(request.idCard());
        draft.setStatus("PENDING_CONFIRM");
        draftRepository.save(draft);

        ReservationToken token = slotStockService.reserve(draft.getSlotId(), 1);
        if (!token.success()) {
            throw new IllegalArgumentException(token.message());
        }

        RegistrationOrderEntity order;
        try {
            RegistrationReservedEvent event = new RegistrationReservedEvent(token.token(), draft.getId(), draft.getSlotId(), draft.getSessionId());
            eventPublisher.publish(event);
            chatHistoryService.storeToolTrace(request.sessionId(), "confirmRegistration",
                    Map.of("draftId", request.draftId(), "slotId", draft.getSlotId(), "idempotencyKey", idempotencyKey(request)),
                    Map.of("token", token.token(), "provider", eventPublisher.providerName()));
            order = waitForOrderByDraftId(draft.getId());
        } catch (RuntimeException ex) {
            slotStockService.release(draft.getSlotId(), token.token(), 1);
            throw ex;
        }

        return toView(order);
    }

    public List<RegistrationOrderView> listOrders(String hospitalId) {
        return orderRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(this::toView)
                .toList();
    }

    public RegistrationDraftView latestDraft(String sessionId) {
        return draftRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .findFirst()
                .map(this::toView)
                .orElse(null);
    }

    private RegistrationOrderEntity waitForOrderByDraftId(String draftId) {
        for (int i = 0; i < 20; i++) {
            RegistrationOrderEntity order = orderRepository.findByDraftId(draftId).orElse(null);
            if (order != null) {
                return order;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for registration order");
            }
        }
        throw new IllegalStateException("registration order has not been created yet");
    }

    private String idempotencyKey(ConfirmRegistrationRequest request) {
        return request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? request.draftId()
                : request.idempotencyKey();
    }

    private void validateDraft(RegistrationDraftEntity draft) {
        if (draft.getDepartmentId() == null || draft.getDepartmentId().isBlank()) {
            throw new IllegalArgumentException("draft is missing department");
        }
        if (draft.getClinicRoomId() == null || draft.getClinicRoomId().isBlank()) {
            throw new IllegalArgumentException("draft is missing clinic room");
        }
        if (draft.getDoctorId() == null || draft.getDoctorId().isBlank()) {
            throw new IllegalArgumentException("draft is missing doctor");
        }
        if (draft.getSlotId() == null || draft.getSlotId().isBlank()) {
            throw new IllegalArgumentException("draft is missing schedule slot");
        }
        if (draft.getVisitDate() == null || draft.getVisitPeriod() == null || draft.getVisitPeriod().isBlank()) {
            throw new IllegalArgumentException("draft is missing visit time");
        }
    }

    private RegistrationDraftView toView(RegistrationDraftEntity entity) {
        return new RegistrationDraftView(
                entity.getId(),
                entity.getSessionId(),
                entity.getSymptomSummary(),
                entity.getDepartmentId(),
                entity.getClinicRoomId(),
                entity.getDoctorId(),
                entity.getSlotId(),
                entity.getVisitDate(),
                entity.getVisitPeriod(),
                entity.getPatientName(),
                entity.getPatientPhone(),
                entity.getIdCard(),
                entity.getStatus()
        );
    }

    private RegistrationOrderView toView(RegistrationOrderEntity entity) {
        return new RegistrationOrderView(
                entity.getId(),
                entity.getOrderNo(),
                entity.getSessionId(),
                entity.getPatientName(),
                entity.getPatientPhone(),
                entity.getDepartmentId(),
                entity.getClinicRoomId(),
                entity.getDoctorId(),
                entity.getSlotId(),
                entity.getVisitDate(),
                entity.getVisitPeriod(),
                entity.getStatus(),
                entity.getSymptomSummary(),
                entity.getCreatedAt()
        );
    }
}

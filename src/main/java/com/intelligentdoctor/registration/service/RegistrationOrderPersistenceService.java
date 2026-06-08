package com.intelligentdoctor.registration.service;

import com.intelligentdoctor.registration.entity.RegistrationDraftEntity;
import com.intelligentdoctor.registration.entity.RegistrationOrderEntity;
import com.intelligentdoctor.registration.event.RegistrationReservedEvent;
import com.intelligentdoctor.registration.repository.RegistrationDraftRepository;
import com.intelligentdoctor.registration.repository.RegistrationOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class RegistrationOrderPersistenceService {

    private final RegistrationDraftRepository draftRepository;
    private final RegistrationOrderRepository orderRepository;

    public RegistrationOrderPersistenceService(RegistrationDraftRepository draftRepository,
                                               RegistrationOrderRepository orderRepository) {
        this.draftRepository = draftRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public RegistrationOrderEntity persist(RegistrationReservedEvent event) {
        RegistrationDraftEntity draft = draftRepository.findById(event.draftId())
                .orElseThrow(() -> new EntityNotFoundException("registration draft not found"));

        return orderRepository.findByDraftId(draft.getId())
                .orElseGet(() -> createOrder(draft));
    }

    private RegistrationOrderEntity createOrder(RegistrationDraftEntity draft) {
        if ("CONFIRMED".equalsIgnoreCase(draft.getStatus())) {
            return orderRepository.findByDraftId(draft.getId())
                    .orElseThrow(() -> new EntityNotFoundException("registration order not found"));
        }

        RegistrationOrderEntity order = new RegistrationOrderEntity();
        order.setHospitalId(draft.getHospitalId());
        order.setOrderNo(generateOrderNo());
        order.setDraftId(draft.getId());
        order.setSessionId(draft.getSessionId());
        order.setDepartmentId(draft.getDepartmentId());
        order.setClinicRoomId(draft.getClinicRoomId());
        order.setDoctorId(draft.getDoctorId());
        order.setSlotId(draft.getSlotId());
        order.setVisitDate(draft.getVisitDate());
        order.setVisitPeriod(draft.getVisitPeriod());
        order.setPatientName(draft.getPatientName());
        order.setPatientPhone(draft.getPatientPhone());
        order.setIdCard(draft.getIdCard());
        order.setGender(draft.getGender());
        order.setAge(draft.getAge());
        order.setStatus("CONFIRMED");
        order.setSymptomSummary(draft.getSymptomSummary());

        try {
            RegistrationOrderEntity saved = orderRepository.save(order);
            draft.setStatus("CONFIRMED");
            draftRepository.save(draft);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return orderRepository.findByDraftId(draft.getId()).orElseThrow(() -> ex);
        }
    }

    private String generateOrderNo() {
        return "IDR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

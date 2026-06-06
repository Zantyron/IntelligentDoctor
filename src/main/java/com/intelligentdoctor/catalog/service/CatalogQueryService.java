package com.intelligentdoctor.catalog.service;

import com.intelligentdoctor.catalog.dto.ClinicRoomView;
import com.intelligentdoctor.catalog.dto.DepartmentView;
import com.intelligentdoctor.catalog.dto.DoctorView;
import com.intelligentdoctor.catalog.dto.RegistrationRuleView;
import com.intelligentdoctor.catalog.dto.ScheduleSlotView;
import com.intelligentdoctor.catalog.entity.DepartmentEntity;
import com.intelligentdoctor.catalog.repository.ClinicRoomRepository;
import com.intelligentdoctor.catalog.repository.DepartmentRepository;
import com.intelligentdoctor.catalog.repository.DoctorRepository;
import com.intelligentdoctor.catalog.repository.RegistrationRuleRepository;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CatalogQueryService {

    private final DepartmentRepository departmentRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RegistrationRuleRepository registrationRuleRepository;

    public CatalogQueryService(DepartmentRepository departmentRepository,
                               ClinicRoomRepository clinicRoomRepository,
                               DoctorRepository doctorRepository,
                               ScheduleSlotRepository scheduleSlotRepository,
                               RegistrationRuleRepository registrationRuleRepository) {
        this.departmentRepository = departmentRepository;
        this.clinicRoomRepository = clinicRoomRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.registrationRuleRepository = registrationRuleRepository;
    }

    public List<DepartmentView> searchDepartments(String hospitalId, String keyword) {
        String normalized = normalize(keyword);
        return departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId).stream()
                .filter(entity -> normalized.isBlank() || contains(entity.getName(), normalized) || contains(entity.getDescription(), normalized))
                .map(entity -> new DepartmentView(entity.getId(), entity.getName(), entity.getDescription(), entity.getCategory()))
                .toList();
    }

    public List<ClinicRoomView> searchClinics(String hospitalId, String departmentId) {
        return clinicRoomRepository.findByHospitalIdAndDepartmentId(hospitalId, departmentId).stream()
                .map(entity -> new ClinicRoomView(entity.getId(), entity.getName(), entity.getLocation(), entity.getDescription()))
                .toList();
    }

    public List<DoctorView> searchDoctors(String hospitalId, String departmentId, String clinicRoomId) {
        if (clinicRoomId != null && !clinicRoomId.isBlank()) {
            return doctorRepository.findByHospitalIdAndClinicRoomId(hospitalId, clinicRoomId).stream()
                    .map(entity -> new DoctorView(entity.getId(), entity.getName(), entity.getTitle(), entity.getSpecialty(),
                            entity.getIntroduction(), Boolean.TRUE.equals(entity.getHotExpert()), entity.getConsultationFee()))
                    .toList();
        }
        return doctorRepository.findByHospitalIdAndDepartmentId(hospitalId, departmentId).stream()
                .map(entity -> new DoctorView(entity.getId(), entity.getName(), entity.getTitle(), entity.getSpecialty(),
                        entity.getIntroduction(), Boolean.TRUE.equals(entity.getHotExpert()), entity.getConsultationFee()))
                .toList();
    }

    public List<ScheduleSlotView> querySchedules(String hospitalId, String departmentId, String doctorId) {
        if (doctorId != null && !doctorId.isBlank()) {
            return scheduleSlotRepository.findByHospitalIdAndDoctorIdAndSlotDateGreaterThanEqual(hospitalId, doctorId, LocalDate.now()).stream()
                    .map(entity -> new ScheduleSlotView(entity.getId(), entity.getDoctorId(), entity.getSlotDate(), entity.getPeriod(),
                            entity.getStockAvailable(), Boolean.TRUE.equals(entity.getHotSlot())))
                    .toList();
        }
        return scheduleSlotRepository.findByHospitalIdAndDepartmentIdAndSlotDateGreaterThanEqual(hospitalId, departmentId, LocalDate.now()).stream()
                .map(entity -> new ScheduleSlotView(entity.getId(), entity.getDoctorId(), entity.getSlotDate(), entity.getPeriod(),
                        entity.getStockAvailable(), Boolean.TRUE.equals(entity.getHotSlot())))
                .toList();
    }

    public List<RegistrationRuleView> queryRegistrationRules(String hospitalId, String departmentId) {
        return registrationRuleRepository.findByHospitalIdAndDepartmentId(hospitalId, departmentId).stream()
                .map(entity -> new RegistrationRuleView(entity.getId(), entity.getRuleText(), entity.getNotice()))
                .toList();
    }

    public DepartmentEntity resolveDepartmentByName(String hospitalId, String name) {
        String normalized = normalize(name);
        return departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId).stream()
                .filter(entity -> contains(entity.getName(), normalized))
                .findFirst()
                .orElse(null);
    }

    private String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase();
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }
}

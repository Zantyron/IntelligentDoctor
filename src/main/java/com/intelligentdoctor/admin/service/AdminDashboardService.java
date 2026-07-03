package com.intelligentdoctor.admin.service;

import com.intelligentdoctor.admin.repository.AdminUserRepository;
import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import com.intelligentdoctor.catalog.repository.DepartmentRepository;
import com.intelligentdoctor.catalog.repository.DoctorRepository;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import com.intelligentdoctor.chat.dto.ChatMessageView;
import com.intelligentdoctor.chat.dto.ChatSessionView;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import com.intelligentdoctor.registration.repository.RegistrationOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final String TERMINAL_ROLE = "TERMINAL_USER";

    private final AdminUserRepository adminUserRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RegistrationOrderRepository registrationOrderRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ChatHistoryService chatHistoryService;

    public AdminDashboardService(AdminUserRepository adminUserRepository,
                                 DepartmentRepository departmentRepository,
                                 DoctorRepository doctorRepository,
                                 ScheduleSlotRepository scheduleSlotRepository,
                                 RegistrationOrderRepository registrationOrderRepository,
                                 KnowledgeChunkRepository knowledgeChunkRepository,
                                 ChatHistoryService chatHistoryService) {
        this.adminUserRepository = adminUserRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.registrationOrderRepository = registrationOrderRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.chatHistoryService = chatHistoryService;
    }

    public Map<String, Object> overview(String hospitalId) {
        List<ChatSessionView> sessions = chatHistoryService.listSessions(hospitalId);
        Map<String, Object> overview = new LinkedHashMap<>();
        int terminalUsers = adminUserRepository.findByHospitalIdAndRoleOrderByCreatedAtDesc(hospitalId, TERMINAL_ROLE).size();
        overview.put("terminalUsers", terminalUsers);
        overview.put("adminUsers", terminalUsers);
        overview.put("departments", departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId).size());
        overview.put("doctors", doctorRepository.findByHospitalId(hospitalId).size());
        overview.put("upcomingSchedules", upcomingSchedules(hospitalId).size());
        overview.put("orders", registrationOrderRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId).size());
        overview.put("knowledgeChunks", knowledgeChunkRepository.findByHospitalId(hospitalId).size());
        overview.put("chatSessions", sessions.size());
        overview.put("latestSessionAt", sessions.stream().map(ChatSessionView::updatedAt).filter(value -> value != null).findFirst().orElse(null));
        return overview;
    }

    public List<Map<String, Object>> departments(String hospitalId) {
        return departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId).stream()
                .map(department -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", department.getId());
                    item.put("code", department.getDepartmentCode());
                    item.put("name", department.getName());
                    item.put("category", department.getCategory());
                    item.put("description", department.getDescription());
                    item.put("doctors", doctorRepository.findByHospitalIdAndDepartmentId(hospitalId, department.getId()).size());
                    item.put("schedules", scheduleSlotRepository.findByHospitalIdAndDepartmentIdAndSlotDateGreaterThanEqual(
                            hospitalId, department.getId(), LocalDate.now()).size());
                    return item;
                })
                .toList();
    }

    public List<Map<String, Object>> doctors(String hospitalId) {
        Map<String, String> departmentNames = new LinkedHashMap<>();
        departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId)
                .forEach(department -> departmentNames.put(department.getId(), department.getName()));
        return doctorRepository.findByHospitalId(hospitalId).stream()
                .map(doctor -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", doctor.getId());
                    item.put("code", doctor.getDoctorCode());
                    item.put("name", doctor.getName());
                    item.put("departmentId", doctor.getDepartmentId());
                    item.put("departmentName", departmentNames.getOrDefault(doctor.getDepartmentId(), "-"));
                    item.put("title", doctor.getTitle());
                    item.put("specialty", doctor.getSpecialty());
                    item.put("hotExpert", Boolean.TRUE.equals(doctor.getHotExpert()));
                    item.put("consultationFee", doctor.getConsultationFee());
                    return item;
                })
                .toList();
    }

    public List<Map<String, Object>> schedules(String hospitalId) {
        Map<String, String> doctorNames = new LinkedHashMap<>();
        doctorRepository.findByHospitalId(hospitalId).forEach(doctor -> doctorNames.put(doctor.getId(), doctor.getName()));
        return upcomingSchedules(hospitalId).stream()
                .map(slot -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", slot.getId());
                    item.put("doctorId", slot.getDoctorId());
                    item.put("doctorName", doctorNames.getOrDefault(slot.getDoctorId(), "-"));
                    item.put("slotDate", slot.getSlotDate());
                    item.put("period", slot.getPeriod());
                    item.put("stockTotal", slot.getStockTotal());
                    item.put("stockAvailable", slot.getStockAvailable());
                    item.put("status", "OPEN");
                    item.put("hotSlot", Boolean.TRUE.equals(slot.getHotSlot()));
                    return item;
                })
                .toList();
    }

    public List<ChatSessionView> chatSessions(String hospitalId) {
        return chatHistoryService.listSessions(hospitalId);
    }

    public List<ChatMessageView> chatMessages(String hospitalId, String sessionId) {
        return chatHistoryService.listMessages(hospitalId, sessionId);
    }

    private List<ScheduleSlotEntity> upcomingSchedules(String hospitalId) {
        return departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId).stream()
                .flatMap(department -> scheduleSlotRepository.findByHospitalIdAndDepartmentIdAndSlotDateGreaterThanEqual(
                        hospitalId, department.getId(), LocalDate.now()).stream())
                .sorted(Comparator.comparing(ScheduleSlotEntity::getSlotDate).thenComparing(ScheduleSlotEntity::getPeriod))
                .toList();
    }
}
